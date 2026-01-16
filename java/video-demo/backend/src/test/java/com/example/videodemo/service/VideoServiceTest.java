package com.example.videodemo.service;

import com.example.videodemo.entity.HlsPackage;
import com.example.videodemo.entity.Video;
import com.example.videodemo.entity.VideoStatus;
import com.example.videodemo.exception.ApiException;
import com.example.videodemo.repository.HlsPackageRepository;
import com.example.videodemo.repository.VideoRepository;
import com.example.videodemo.support.RecordingHlsSegmenter;
import com.example.videodemo.support.RecordingProxyVideoGenerator;
import com.example.videodemo.support.TestHlsSegmenterConfig;
import com.example.videodemo.support.TestProxyVideoGeneratorConfig;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import({TestHlsSegmenterConfig.class, TestProxyVideoGeneratorConfig.class})
class VideoServiceTest {

	private static final Path BASE_DIR = Paths.get("target/test-data-service").toAbsolutePath();
	private static final byte[] MP4_HEADER = new byte[] { 0, 0, 0, 18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm' };

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("app.storage-root", () -> BASE_DIR.resolve("videos").toString());
		registry.add("app.hls-root", () -> BASE_DIR.resolve("hls").toString());
		registry.add("app.hls-segment-seconds", () -> 2);
		registry.add("app.db-path", () -> BASE_DIR.resolve("app.db").toString());
		registry.add("app.upload-max-bytes", () -> 100L);
		registry.add("spring.servlet.multipart.location", () -> BASE_DIR.resolve("tmp").toString());
	}

	@Autowired
	private VideoService service;

	@Autowired
	private VideoRepository repository;

	@Autowired
	private HlsPackageRepository hlsRepository;

	@Autowired
	private RecordingHlsSegmenter recordingSegmenter;

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
		repository.deleteAll();
		recordingSegmenter.reset();
	}

	@Test
	void uploadShouldMarkFailedOnStorageError() {
		MultipartFile file = new FailingMultipartFile();

		ApiException exception = assertThrows(ApiException.class, () -> service.upload(file, "Demo"));

		assertThat(exception.getCode()).isEqualTo("STORAGE_IO_ERROR");
		assertThat(repository.count()).isEqualTo(1);
		Video saved = repository.findAll().get(0);
		assertThat(saved.getStatus()).isEqualTo(VideoStatus.FAILED);
		assertThat(saved.getErrorCode()).isEqualTo("STORAGE_IO_ERROR");
		assertThat(saved.getErrorMessage()).isNotBlank();
	}

	@Test
	void uploadShouldTriggerAsyncSegmentation() throws InterruptedException {
		MultipartFile file = new SimpleMultipartFile();

		Video saved = service.upload(file, "Demo");

		waitForSegmenterCalls(1, Duration.ofSeconds(2));
		assertThat(recordingSegmenter.getCallCount()).isEqualTo(1);
		assertThat(recordingSegmenter.getLastSegmentSeconds()).isEqualTo(2);
		assertThat(saved.getStatus()).isEqualTo(VideoStatus.UPLOADING);
	}

	@Test
	void deleteShouldRemoveVideoAndFiles() throws IOException {
		Video video = new Video();
		video.setTitle("Test");
		video.setOriginalFilename("test.mp4");
		video.setSizeBytes(100L);
		video.setStatus(VideoStatus.READY);
		Path storagePath = BASE_DIR.resolve("videos").resolve("1").resolve("test.mp4");
		Files.createDirectories(storagePath.getParent());
		Files.write(storagePath, MP4_HEADER);
		video.setStoragePath(storagePath.toString());

		// 创建代理视频文件
		Path proxyPath = storagePath.getParent().resolve("proxy.mp4");
		Files.write(proxyPath, new byte[] { 0, 1, 2, 3 });
		video.setProxyPath(proxyPath.toString());

		video.setCreatedAt(System.currentTimeMillis());
		video.setUpdatedAt(System.currentTimeMillis());
		Video saved = repository.save(video);

		HlsPackage hlsPackage = new HlsPackage();
		hlsPackage.setVideoId(saved.getId());
		hlsPackage.setManifestPath(storagePath.getParent().resolve("hls").resolve("index.m3u8").toString());
		Path segmentDir = storagePath.getParent().resolve("hls");
		Files.createDirectories(segmentDir);
		Files.write(segmentDir.resolve("seg_00001.ts"), MP4_HEADER);
		hlsPackage.setSegmentDir(segmentDir.toString());
		hlsPackage.setSegmentPattern("seg_%05d.ts");
		hlsPackage.setSegmentDurationSeconds(2);
		hlsPackage.setSegmentCount(1);
		hlsPackage.setTotalDurationSeconds(2);
		hlsPackage.setGeneratedAt(System.currentTimeMillis());
		hlsRepository.save(hlsPackage);

		service.delete(saved.getId());

		assertThat(repository.findById(saved.getId())).isEmpty();
		assertThat(hlsRepository.findById(saved.getId())).isEmpty();
		assertThat(Files.exists(storagePath)).isFalse();
		assertThat(Files.exists(proxyPath)).isFalse();
		assertThat(Files.exists(segmentDir)).isFalse();
	}

	@Test
	void deleteShouldThrowWhenVideoNotFound() {
		ApiException exception = assertThrows(ApiException.class, () -> service.delete(999L));

		assertThat(exception.getCode()).isEqualTo("VIDEO_NOT_FOUND");
		assertThat(exception.getStatus().value()).isEqualTo(404);
	}

	@Test
	void deleteShouldSucceedWhenHlsPackageMissing() throws IOException {
		Video video = new Video();
		video.setTitle("Test");
		video.setOriginalFilename("test.mp4");
		video.setSizeBytes(100L);
		video.setStatus(VideoStatus.UPLOADING);
		Path storagePath = BASE_DIR.resolve("videos").resolve("2").resolve("test.mp4");
		Files.createDirectories(storagePath.getParent());
		Files.write(storagePath, MP4_HEADER);
		video.setStoragePath(storagePath.toString());
		video.setCreatedAt(System.currentTimeMillis());
		video.setUpdatedAt(System.currentTimeMillis());
		Video saved = repository.save(video);

		service.delete(saved.getId());

		assertThat(repository.findById(saved.getId())).isEmpty();
		assertThat(Files.exists(storagePath)).isFalse();
	}

	@Test
	void deleteShouldStillDeleteDatabaseRecordWhenFileDeleteFails() throws IOException {
		Video video = new Video();
		video.setTitle("Test");
		video.setOriginalFilename("test.mp4");
		video.setSizeBytes(100L);
		video.setStatus(VideoStatus.READY);
		Path storagePath = BASE_DIR.resolve("videos").resolve("3").resolve("test.mp4");
		Files.createDirectories(storagePath.getParent());
		Files.write(storagePath, MP4_HEADER);
		video.setStoragePath(storagePath.toString());
		video.setCreatedAt(System.currentTimeMillis());
		video.setUpdatedAt(System.currentTimeMillis());
		Video saved = repository.save(video);

		HlsPackage hlsPackage = new HlsPackage();
		hlsPackage.setVideoId(saved.getId());
		hlsPackage.setManifestPath(storagePath.getParent().resolve("hls").resolve("index.m3u8").toString());
		Path segmentDir = storagePath.getParent().resolve("hls");
		Files.createDirectories(segmentDir);
		Files.write(segmentDir.resolve("seg_00001.ts"), MP4_HEADER);
		hlsPackage.setSegmentDir(segmentDir.toString());
		hlsPackage.setSegmentPattern("seg_%05d.ts");
		hlsPackage.setSegmentDurationSeconds(2);
		hlsPackage.setSegmentCount(1);
		hlsPackage.setTotalDurationSeconds(2);
		hlsPackage.setGeneratedAt(System.currentTimeMillis());
		hlsRepository.save(hlsPackage);

		Files.delete(storagePath);

		service.delete(saved.getId());

		assertThat(repository.findById(saved.getId())).isEmpty();
		assertThat(hlsRepository.findById(saved.getId())).isEmpty();
	}

	private void waitForSegmenterCalls(int expected, Duration timeout) throws InterruptedException {
		long deadline = System.nanoTime() + timeout.toNanos();
		while (System.nanoTime() < deadline) {
			if (recordingSegmenter.getCallCount() >= expected) {
				return;
			}
			Thread.sleep(50);
		}
	}

	private static final class FailingMultipartFile implements MultipartFile {

		private final AtomicInteger calls = new AtomicInteger();

		@Override
		public String getName() {
			return "file";
		}

		@Override
		public String getOriginalFilename() {
			return "demo.mp4";
		}

		@Override
		public String getContentType() {
			return "video/mp4";
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public long getSize() {
			return MP4_HEADER.length;
		}

		@Override
		public byte[] getBytes() {
			return MP4_HEADER.clone();
		}

		@Override
		public InputStream getInputStream() throws IOException {
			if (calls.getAndIncrement() == 0) {
				return new ByteArrayInputStream(MP4_HEADER);
			}
			return new InputStream() {
				@Override
				public int read() throws IOException {
					throw new IOException("Storage failure");
				}
			};
		}

		@Override
		public void transferTo(File dest) throws IOException {
			throw new IOException("Storage failure");
		}
	}

	private static final class SimpleMultipartFile implements MultipartFile {

		@Override
		public String getName() {
			return "file";
		}

		@Override
		public String getOriginalFilename() {
			return "demo.mp4";
		}

		@Override
		public String getContentType() {
			return "video/mp4";
		}

		@Override
		public boolean isEmpty() {
			return false;
		}

		@Override
		public long getSize() {
			return MP4_HEADER.length;
		}

		@Override
		public byte[] getBytes() {
			return MP4_HEADER.clone();
		}

		@Override
		public InputStream getInputStream() {
			return new ByteArrayInputStream(MP4_HEADER);
		}

		@Override
		public void transferTo(File dest) throws IOException {
			Files.write(dest.toPath(), MP4_HEADER);
		}
	}
}
