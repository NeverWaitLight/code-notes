package com.example.videodemo.service;

import com.example.videodemo.entity.HlsPackage;
import com.example.videodemo.entity.Video;
import com.example.videodemo.entity.VideoStatus;
import com.example.videodemo.repository.HlsPackageRepository;
import com.example.videodemo.repository.VideoRepository;
import com.example.videodemo.support.RecordingHlsSegmenter;
import com.example.videodemo.support.RecordingProxyVideoGenerator;
import com.example.videodemo.support.TestHlsSegmenterConfig;
import com.example.videodemo.support.TestProxyVideoGeneratorConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import({TestHlsSegmenterConfig.class, TestProxyVideoGeneratorConfig.class})
class VideoProcessingServiceTest {

	private static final Path BASE_DIR = Paths.get("target/test-data-processing").toAbsolutePath();

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("app.storage-root", () -> BASE_DIR.resolve("videos").toString());
		registry.add("app.hls-root", () -> BASE_DIR.resolve("hls").toString());
		registry.add("app.hls-segment-seconds", () -> 2);
		registry.add("app.db-path", () -> BASE_DIR.resolve("app.db").toString());
		registry.add("spring.servlet.multipart.location", () -> BASE_DIR.resolve("tmp").toString());
	}

	@Autowired
	private VideoProcessingService processingService;

	@Autowired
	private VideoRepository videoRepository;

	@Autowired
	private HlsPackageRepository hlsRepository;

	@Autowired
	private RecordingHlsSegmenter recordingSegmenter;

	@Autowired
	private RecordingProxyVideoGenerator recordingProxyGenerator;

	@BeforeAll
	static void prepareDirectories() throws IOException {
		Files.createDirectories(BASE_DIR);
		Files.createDirectories(BASE_DIR.resolve("videos"));
		Files.createDirectories(BASE_DIR.resolve("hls"));
		Files.createDirectories(BASE_DIR.resolve("tmp"));
	}

	@BeforeEach
	void cleanDatabase() {
		hlsRepository.deleteAll();
		videoRepository.deleteAll();
		recordingSegmenter.reset();
		recordingProxyGenerator.reset();
	}

	@Test
	void processInternalShouldPersistPackageAndMarkReady() throws IOException {
		Video video = new Video();
		video.setTitle("Sample");
		video.setOriginalFilename("sample.mp4");
		video.setSizeBytes(10L);
		video.setStatus(VideoStatus.UPLOADING);
		video.setStoragePath("pending");
		video.setCreatedAt(1L);
		video.setUpdatedAt(1L);
		Video saved = videoRepository.save(video);

		Path videoPath = BASE_DIR.resolve("videos")
				.resolve(String.valueOf(saved.getId()))
				.resolve("sample.mp4");
		Files.createDirectories(videoPath.getParent());
		Files.write(videoPath, new byte[] { 0 });
		saved.setStoragePath(videoPath.toString());
		videoRepository.save(saved);

		processingService.processInternal(saved.getId());

		Video updated = videoRepository.findById(saved.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(VideoStatus.READY);
		assertThat(updated.getErrorCode()).isNull();
		assertThat(updated.getProxyPath()).isNotNull();

		// 验证代理视频生成被调用
		assertThat(recordingProxyGenerator.getCallCount()).isEqualTo(1);
		assertThat(recordingProxyGenerator.getLastInputPath()).isEqualTo(videoPath);

		HlsPackage hlsPackage = hlsRepository.findById(saved.getId()).orElseThrow();
		assertThat(hlsPackage.getManifestPath()).isNotBlank();
		assertThat(hlsPackage.getSegmentCount()).isEqualTo(1);
		assertThat(hlsPackage.getSegmentPattern()).isEqualTo("seg_%05d.ts");

		// 验证 HLS 分片使用代理视频作为输入
		assertThat(recordingSegmenter.getCallCount()).isEqualTo(1);
		assertThat(recordingSegmenter.getLastSegmentSeconds()).isEqualTo(2);
		assertThat(recordingSegmenter.getLastInput()).isEqualTo(recordingProxyGenerator.getLastOutputPath());
	}

	@Test
	void processInternalShouldMarkFailedWhenSourceMissing() {
		Video video = new Video();
		video.setTitle("Missing");
		video.setOriginalFilename("missing.mp4");
		video.setSizeBytes(10L);
		video.setStatus(VideoStatus.UPLOADING);
		video.setStoragePath(BASE_DIR.resolve("videos/missing.mp4").toString());
		video.setCreatedAt(1L);
		video.setUpdatedAt(1L);
		Video saved = videoRepository.save(video);

		processingService.processInternal(saved.getId());

		Video updated = videoRepository.findById(saved.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(VideoStatus.FAILED);
		assertThat(updated.getErrorCode()).isEqualTo("PROCESSING_FAILED");
		assertThat(updated.getErrorMessage()).isNotBlank();
	}

	@Test
	void processInternalShouldMarkFailedWhenProxyGenerationFails() throws IOException {
		Video video = new Video();
		video.setTitle("ProxyFail");
		video.setOriginalFilename("proxyfail.mp4");
		video.setSizeBytes(10L);
		video.setStatus(VideoStatus.UPLOADING);
		video.setStoragePath("pending");
		video.setCreatedAt(1L);
		video.setUpdatedAt(1L);
		Video saved = videoRepository.save(video);

		Path videoPath = BASE_DIR.resolve("videos")
				.resolve(String.valueOf(saved.getId()))
				.resolve("proxyfail.mp4");
		Files.createDirectories(videoPath.getParent());
		Files.write(videoPath, new byte[] { 0 });
		saved.setStoragePath(videoPath.toString());
		videoRepository.save(saved);

		// 设置代理视频生成失败
		recordingProxyGenerator.setShouldFail(true);

		processingService.processInternal(saved.getId());

		Video updated = videoRepository.findById(saved.getId()).orElseThrow();
		assertThat(updated.getStatus()).isEqualTo(VideoStatus.FAILED);
		assertThat(updated.getErrorCode()).isEqualTo("PROXY_GENERATION_FAILED");
		assertThat(updated.getErrorMessage()).contains("Proxy video generation failed");

		// 验证代理视频生成被调用但失败
		assertThat(recordingProxyGenerator.getCallCount()).isEqualTo(1);

		// 验证 HLS 分片未被调用
		assertThat(recordingSegmenter.getCallCount()).isEqualTo(0);
	}
}
