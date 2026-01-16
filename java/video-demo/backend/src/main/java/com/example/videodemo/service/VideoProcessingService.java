package com.example.videodemo.service;

import com.example.videodemo.config.AppProperties;
import com.example.videodemo.entity.HlsPackage;
import com.example.videodemo.entity.Video;
import com.example.videodemo.entity.VideoStatus;
import com.example.videodemo.repository.HlsPackageRepository;
import com.example.videodemo.repository.VideoRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Comparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class VideoProcessingService {

	private static final String SEGMENT_PATTERN = "seg_%05d.ts";
	private static final String MANIFEST_FILENAME = "index.m3u8";

	private final VideoRepository videoRepository;
	private final HlsPackageRepository hlsRepository;
	private final AppProperties properties;
	private final HlsSegmenter segmenter;

	public VideoProcessingService(VideoRepository videoRepository,
			HlsPackageRepository hlsRepository,
			AppProperties properties,
			HlsSegmenter segmenter) {
		this.videoRepository = videoRepository;
		this.hlsRepository = hlsRepository;
		this.properties = properties;
		this.segmenter = segmenter;
	}

	@Async
	public void process(Long videoId) {
		processInternal(videoId);
	}

	void processInternal(Long videoId) {
		if (videoId == null) {
			return;
		}
		Video video = videoRepository.findById(videoId).orElse(null);
		if (video == null) {
			return;
		}

		Path outputDir = resolveHlsDirectory(videoId);
		try {
			Path sourcePath = resolveSourcePath(video.getStoragePath());
			if (Files.notExists(sourcePath)) {
				throw new IOException("Source file missing");
			}
			int segmentSeconds = properties.getHlsSegmentSeconds();
			if (segmentSeconds <= 0) {
				segmentSeconds = 2;
			}
			HlsSegmentResult result = segmenter.segment(sourcePath, outputDir, segmentSeconds, SEGMENT_PATTERN);

			HlsPackage hlsPackage = new HlsPackage();
			hlsPackage.setVideoId(video.getId());
			hlsPackage.setManifestPath(result.manifestPath().toString());
			hlsPackage.setSegmentDir(outputDir.toString());
			hlsPackage.setSegmentPattern(SEGMENT_PATTERN);
			hlsPackage.setSegmentDurationSeconds(segmentSeconds);
			hlsPackage.setSegmentCount(result.segmentCount());
			hlsPackage.setTotalDurationSeconds(result.totalDurationSeconds());
			hlsPackage.setGeneratedAt(Instant.now().toEpochMilli());
			hlsRepository.save(hlsPackage);

			video.setStatus(VideoStatus.READY);
			video.setErrorCode(null);
			video.setErrorMessage(null);
			videoRepository.save(video);
		} catch (Exception ex) {
			markFailed(video, ex);
			cleanupDirectory(outputDir);
		}
	}

	private Path resolveSourcePath(String storagePathValue) {
		Path storagePath = Paths.get(storagePathValue);
		if (storagePath.isAbsolute()) {
			return storagePath.normalize();
		}
		return Paths.get("").toAbsolutePath().resolve(storagePath).normalize();
	}

	private Path resolveHlsDirectory(Long videoId) {
		Path storageRoot = resolveStorageRoot();
		return storageRoot.resolve(String.valueOf(videoId)).resolve("hls").normalize();
	}

	private Path resolveStorageRoot() {
		Path rootPath = Paths.get(properties.getStorageRoot());
		if (rootPath.isAbsolute()) {
			return rootPath.normalize();
		}
		return Paths.get("").toAbsolutePath().resolve(rootPath).normalize();
	}

	private void markFailed(Video video, Exception ex) {
		video.setStatus(VideoStatus.FAILED);
		video.setErrorCode("PROCESSING_FAILED");
		video.setErrorMessage(safeMessage(ex));
		videoRepository.save(video);
	}

	private void cleanupDirectory(Path directory) {
		if (directory == null || Files.notExists(directory)) {
			return;
		}
		try (var stream = Files.walk(directory)) {
			stream.sorted(Comparator.reverseOrder())
					.forEach(path -> {
						try {
							Files.deleteIfExists(path);
						} catch (IOException ignore) {
							// best-effort cleanup
						}
					});
		} catch (IOException ignore) {
			// best-effort cleanup
		}
	}

	private String safeMessage(Exception ex) {
		String message = ex.getMessage();
		return message == null ? "Processing failed" : message;
	}
}
