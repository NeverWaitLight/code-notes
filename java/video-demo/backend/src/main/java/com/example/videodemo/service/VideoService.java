package com.example.videodemo.service;

import com.example.videodemo.config.AppProperties;
import com.example.videodemo.dto.VideoDetailDto;
import com.example.videodemo.entity.HlsPackage;
import com.example.videodemo.entity.Video;
import com.example.videodemo.entity.VideoStatus;
import com.example.videodemo.exception.ApiException;
import com.example.videodemo.repository.HlsPackageRepository;
import com.example.videodemo.repository.VideoRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class VideoService {

	private final VideoRepository repository;
	private final HlsPackageRepository hlsRepository;
	private final AppProperties properties;
	private final VideoProcessingService processingService;

	public VideoService(VideoRepository repository,
			HlsPackageRepository hlsRepository,
			AppProperties properties,
			VideoProcessingService processingService) {
		this.repository = repository;
		this.hlsRepository = hlsRepository;
		this.properties = properties;
		this.processingService = processingService;
	}

	public Video upload(MultipartFile file, String title) {
		validateRequest(file, title);

		long sizeBytes = file.getSize();
		Video video = new Video();
		video.setTitle(title.trim());
		video.setOriginalFilename(safeFilename(file.getOriginalFilename()));
		video.setSizeBytes(sizeBytes);
		video.setStatus(VideoStatus.UPLOADING);
		video.setStoragePath("pending");

		Video saved = repository.save(video);
		Path target = resolveStoragePath(saved.getId(), saved.getOriginalFilename());

		try {
			Files.createDirectories(target.getParent());
			try (InputStream inputStream = file.getInputStream()) {
				Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException ex) {
			markFailed(saved, "STORAGE_IO_ERROR", safeMessage(ex));
			throw new ApiException("STORAGE_IO_ERROR", "Failed to store file", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		saved.setStoragePath(target.toString());
		saved.setErrorCode(null);
		saved.setErrorMessage(null);
		Video persisted = repository.save(saved);
		processingService.process(persisted.getId());
		return persisted;
	}

	public List<Video> list(int page, int size) {
		if (page < 0 || size <= 0) {
			throw new ApiException("INVALID_REQUEST", "Invalid pagination", HttpStatus.BAD_REQUEST);
		}
		return repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
	}

	public VideoDetailDto getDetail(long id) {
		Video video = repository.findById(id)
				.orElseThrow(() -> new ApiException("VIDEO_NOT_FOUND", "Video not found", HttpStatus.NOT_FOUND));
		if (video.getStatus() != VideoStatus.READY) {
			throw new ApiException("HLS_NOT_READY", "HLS is not ready", HttpStatus.CONFLICT);
		}
		HlsPackage hlsPackage = hlsRepository.findById(id).orElse(null);
		if (hlsPackage == null) {
			throw new ApiException("HLS_NOT_READY", "HLS is not ready", HttpStatus.CONFLICT);
		}
		String manifestUrl = buildManifestUrl(id);
		return new VideoDetailDto(
				video.getId(),
				video.getTitle(),
				video.getStatus(),
				video.getSizeBytes(),
				video.getCreatedAt(),
				manifestUrl);
	}

	private void validateRequest(MultipartFile file, String title) {
		if (file == null || file.isEmpty()) {
			throw new ApiException("INVALID_REQUEST", "File is required", HttpStatus.BAD_REQUEST);
		}
		if (title == null || title.isBlank()) {
			throw new ApiException("INVALID_REQUEST", "Title is required", HttpStatus.BAD_REQUEST);
		}
		long maxBytes = properties.getUploadMaxBytes();
		if (maxBytes > 0 && file.getSize() > maxBytes) {
			throw new ApiException("UPLOAD_TOO_LARGE", "Upload exceeds size limit", HttpStatus.PAYLOAD_TOO_LARGE);
		}
		String filename = safeFilename(file.getOriginalFilename());
		String contentType = file.getContentType();
		boolean contentTypeOk = contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("video/mp4");
		boolean extensionOk = filename.toLowerCase(Locale.ROOT).endsWith(".mp4");
		if (!contentTypeOk && !extensionOk) {
			throw new ApiException("INVALID_MEDIA_TYPE", "Only MP4 is supported", HttpStatus.BAD_REQUEST);
		}
		if (!hasMp4Signature(file)) {
			throw new ApiException("INVALID_MEDIA_TYPE", "Only MP4 is supported", HttpStatus.BAD_REQUEST);
		}
	}

	private String safeFilename(String originalFilename) {
		String filename = originalFilename == null ? "video.mp4" : originalFilename;
		String normalized = Paths.get(filename).getFileName().toString();
		return normalized.isBlank() ? "video.mp4" : normalized;
	}

	private Path resolveStoragePath(Long id, String filename) {
		String rootValue = properties.getStorageRoot();
		Path rootPath = Paths.get(rootValue);
		if (!rootPath.isAbsolute()) {
			rootPath = Paths.get("").toAbsolutePath().resolve(rootPath).normalize();
		}
		return rootPath.resolve(String.valueOf(id)).resolve(filename).normalize();
	}

	private boolean hasMp4Signature(MultipartFile file) {
		try (InputStream inputStream = file.getInputStream()) {
			byte[] header = inputStream.readNBytes(12);
			if (header.length < 12) {
				return false;
			}
			return header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p';
		} catch (IOException ex) {
			throw new ApiException("INVALID_REQUEST", "Failed to read upload", HttpStatus.BAD_REQUEST);
		}
	}

	private void markFailed(Video video, String code, String message) {
		video.setStatus(VideoStatus.FAILED);
		video.setErrorCode(code);
		video.setErrorMessage(message);
		repository.save(video);
	}

	private String safeMessage(IOException ex) {
		String message = ex.getMessage();
		return message == null ? "Storage IO error" : message;
	}

	private String buildManifestUrl(long id) {
		return "http://localhost:8080/media/" + id + "/index.m3u8";
	}

	public void delete(long id) {
		Video video = repository.findById(id)
				.orElseThrow(() -> new ApiException("VIDEO_NOT_FOUND", "Video not found", HttpStatus.NOT_FOUND));

		HlsPackage hlsPackage = hlsRepository.findById(id).orElse(null);
		String segmentDir = hlsPackage != null ? hlsPackage.getSegmentDir() : null;

		IOException fileDeleteException = null;
		try {
			Path storagePath = resolveSourcePath(video.getStoragePath());
			Files.deleteIfExists(storagePath);

			if (segmentDir != null) {
				Path segmentDirPath = Paths.get(segmentDir);
				if (segmentDirPath.isAbsolute()) {
					segmentDirPath = segmentDirPath.normalize();
				} else {
					segmentDirPath = Paths.get("").toAbsolutePath().resolve(segmentDirPath).normalize();
				}
				if (Files.exists(segmentDirPath)) {
					try (var stream = Files.walk(segmentDirPath)) {
						stream.sorted(Comparator.reverseOrder())
								.forEach(path -> {
									try {
										Files.deleteIfExists(path);
									} catch (IOException ignore) {
									}
								});
					}
				}
			}
		} catch (IOException ex) {
			fileDeleteException = ex;
		}

		if (hlsPackage != null) {
			hlsRepository.delete(hlsPackage);
		}
		repository.delete(video);

		if (fileDeleteException != null) {
			throw new ApiException("STORAGE_IO_ERROR", safeMessage(fileDeleteException), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	private Path resolveSourcePath(String storagePathValue) {
		Path storagePath = Paths.get(storagePathValue);
		if (storagePath.isAbsolute()) {
			return storagePath.normalize();
		}
		return Paths.get("").toAbsolutePath().resolve(storagePath).normalize();
	}
}
