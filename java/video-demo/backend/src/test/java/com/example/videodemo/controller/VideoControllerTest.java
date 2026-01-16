package com.example.videodemo.controller;

import com.example.videodemo.entity.Video;
import com.example.videodemo.entity.VideoStatus;
import com.example.videodemo.entity.HlsPackage;
import com.example.videodemo.repository.HlsPackageRepository;
import com.example.videodemo.repository.VideoRepository;
import com.example.videodemo.support.TestHlsSegmenterConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestHlsSegmenterConfig.class)
class VideoControllerTest {

	private static final Path BASE_DIR = Paths.get("target/test-data").toAbsolutePath();

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("app.storage-root", () -> BASE_DIR.resolve("videos").toString());
		registry.add("app.hls-root", () -> BASE_DIR.resolve("hls").toString());
		registry.add("app.hls-segment-seconds", () -> 2);
		registry.add("app.db-path", () -> BASE_DIR.resolve("app.db").toString());
		registry.add("app.upload-max-bytes", () -> 20L);
		registry.add("spring.servlet.multipart.location", () -> BASE_DIR.resolve("tmp").toString());
	}

	@Autowired
	private MockMvc mvc;

	@Autowired
	private VideoRepository repository;

	@Autowired
	private HlsPackageRepository hlsRepository;

	@BeforeAll
	static void prepareDirectories() throws IOException {
		Files.createDirectories(BASE_DIR);
		Files.createDirectories(BASE_DIR.resolve("videos"));
		Files.createDirectories(BASE_DIR.resolve("hls"));
		Files.createDirectories(BASE_DIR.resolve("tmp"));
	}

	@BeforeEach
	void setUp() {
		repository.deleteAll();
		hlsRepository.deleteAll();
	}

	@Test
	void uploadShouldCreateVideo() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"demo.mp4",
				"video/mp4",
				validMp4Bytes());

		mvc.perform(multipart("/api/videos")
						.file(file)
						.param("title", "Demo"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.title").value("Demo"))
				.andExpect(jsonPath("$.status").value("UPLOADING"))
				.andExpect(jsonPath("$.sizeBytes").value(12))
				.andExpect(jsonPath("$.createdAt").isNumber());

		assertThat(repository.count()).isEqualTo(1);
		Video saved = repository.findAll().get(0);
		assertThat(saved.getStoragePath()).isNotBlank();
		assertThat(Files.exists(Path.of(saved.getStoragePath()))).isTrue();
	}

	@Test
	void uploadShouldRejectMissingTitle() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"demo.mp4",
				"video/mp4",
				validMp4Bytes());

		mvc.perform(multipart("/api/videos").file(file))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void uploadShouldRejectMissingFile() throws Exception {
		mvc.perform(multipart("/api/videos").param("title", "Demo"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
	}

	@Test
	void uploadShouldRejectInvalidMediaType() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"demo.bin",
				MediaType.APPLICATION_OCTET_STREAM_VALUE,
				new byte[] { 1, 2, 3 });

		mvc.perform(multipart("/api/videos")
						.file(file)
						.param("title", "Demo"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_MEDIA_TYPE"));
	}

	@Test
	void uploadShouldRejectTooLarge() throws Exception {
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"demo.mp4",
				"video/mp4",
				new byte[21]);

		mvc.perform(multipart("/api/videos")
						.file(file)
						.param("title", "Demo"))
				.andExpect(status().isPayloadTooLarge())
				.andExpect(jsonPath("$.error.code").value("UPLOAD_TOO_LARGE"));
	}

	@Test
	void listShouldReturnRequiredFields() throws Exception {
		Video video = new Video();
		video.setTitle("Sample");
		video.setOriginalFilename("sample.mp4");
		video.setSizeBytes(12L);
		video.setStatus(VideoStatus.READY);
		video.setStoragePath("/tmp/sample.mp4");
		video.setCreatedAt(123L);
		video.setUpdatedAt(123L);
		repository.save(video);

		mvc.perform(get("/api/videos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").isNumber())
				.andExpect(jsonPath("$[0].title").value("Sample"))
				.andExpect(jsonPath("$[0].status").value("READY"))
				.andExpect(jsonPath("$[0].sizeBytes").value(12))
				.andExpect(jsonPath("$[0].createdAt").value(123))
				.andExpect(jsonPath("$[0].storagePath").doesNotExist());
	}

	@Test
	void listShouldSortByCreatedAtDesc() throws Exception {
		Video older = new Video();
		older.setTitle("Older");
		older.setOriginalFilename("older.mp4");
		older.setSizeBytes(1L);
		older.setStatus(VideoStatus.READY);
		older.setStoragePath("/tmp/older.mp4");
		older.setCreatedAt(100L);
		older.setUpdatedAt(100L);
		repository.save(older);

		Video newer = new Video();
		newer.setTitle("Newer");
		newer.setOriginalFilename("newer.mp4");
		newer.setSizeBytes(2L);
		newer.setStatus(VideoStatus.READY);
		newer.setStoragePath("/tmp/newer.mp4");
		newer.setCreatedAt(200L);
		newer.setUpdatedAt(200L);
		repository.save(newer);

		mvc.perform(get("/api/videos").param("page", "0").param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Newer"))
				.andExpect(jsonPath("$[1].title").value("Older"));
	}

	@Test
	void detailShouldReturnManifestUrlWhenReady() throws Exception {
		Video video = new Video();
		video.setTitle("Sample");
		video.setOriginalFilename("sample.mp4");
		video.setSizeBytes(12L);
		video.setStatus(VideoStatus.READY);
		video.setStoragePath("/tmp/sample.mp4");
		video.setCreatedAt(123L);
		video.setUpdatedAt(123L);
		Video saved = repository.save(video);

		HlsPackage hlsPackage = new HlsPackage();
		hlsPackage.setVideoId(saved.getId());
		hlsPackage.setManifestPath("/tmp/index.m3u8");
		hlsPackage.setSegmentDir("/tmp");
		hlsPackage.setSegmentPattern("seg_%05d.ts");
		hlsPackage.setSegmentDurationSeconds(2);
		hlsPackage.setSegmentCount(1);
		hlsPackage.setTotalDurationSeconds(2);
		hlsPackage.setGeneratedAt(123L);
		hlsRepository.save(hlsPackage);

		mvc.perform(get("/api/videos/{id}", saved.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(saved.getId()))
				.andExpect(jsonPath("$.title").value("Sample"))
				.andExpect(jsonPath("$.status").value("READY"))
				.andExpect(jsonPath("$.sizeBytes").value(12))
				.andExpect(jsonPath("$.createdAt").value(123))
				.andExpect(jsonPath("$.manifestUrl")
						.value("http://localhost:8080/media/" + saved.getId() + "/index.m3u8"));
	}

	@Test
	void detailShouldReturnNotReadyWhenProcessing() throws Exception {
		Video video = new Video();
		video.setTitle("Pending");
		video.setOriginalFilename("pending.mp4");
		video.setSizeBytes(12L);
		video.setStatus(VideoStatus.UPLOADING);
		video.setStoragePath("/tmp/pending.mp4");
		video.setCreatedAt(123L);
		video.setUpdatedAt(123L);
		Video saved = repository.save(video);

		mvc.perform(get("/api/videos/{id}", saved.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("HLS_NOT_READY"));
	}

	@Test
	void detailShouldReturnNotReadyWhenFailed() throws Exception {
		Video video = new Video();
		video.setTitle("Failed");
		video.setOriginalFilename("failed.mp4");
		video.setSizeBytes(12L);
		video.setStatus(VideoStatus.FAILED);
		video.setErrorCode("PROCESSING_FAILED");
		video.setErrorMessage("boom");
		video.setStoragePath("/tmp/failed.mp4");
		video.setCreatedAt(123L);
		video.setUpdatedAt(123L);
		Video saved = repository.save(video);

		mvc.perform(get("/api/videos/{id}", saved.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("HLS_NOT_READY"));
	}

	@Test
	void mediaMappingShouldServeManifest() throws Exception {
		Path manifest = BASE_DIR.resolve("videos")
				.resolve("99")
				.resolve("hls")
				.resolve("index.m3u8");
		Files.createDirectories(manifest.getParent());
		Files.writeString(manifest, "#EXTM3U\n");

		mvc.perform(get("/media/99/index.m3u8"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("#EXTM3U")));
	}

	@Test
	void deleteShouldReturnNoContentWhenSuccessful() throws Exception {
		Video video = new Video();
		video.setTitle("ToDelete");
		video.setOriginalFilename("delete.mp4");
		video.setSizeBytes(12L);
		video.setStatus(VideoStatus.READY);
		Path storagePath = BASE_DIR.resolve("videos").resolve("10").resolve("delete.mp4");
		Files.createDirectories(storagePath.getParent());
		Files.write(storagePath, validMp4Bytes());
		video.setStoragePath(storagePath.toString());
		video.setCreatedAt(123L);
		video.setUpdatedAt(123L);
		Video saved = repository.save(video);

		HlsPackage hlsPackage = new HlsPackage();
		hlsPackage.setVideoId(saved.getId());
		hlsPackage.setManifestPath(storagePath.getParent().resolve("hls").resolve("index.m3u8").toString());
		Path segmentDir = storagePath.getParent().resolve("hls");
		Files.createDirectories(segmentDir);
		Files.write(segmentDir.resolve("seg_00001.ts"), validMp4Bytes());
		hlsPackage.setSegmentDir(segmentDir.toString());
		hlsPackage.setSegmentPattern("seg_%05d.ts");
		hlsPackage.setSegmentDurationSeconds(2);
		hlsPackage.setSegmentCount(1);
		hlsPackage.setTotalDurationSeconds(2);
		hlsPackage.setGeneratedAt(123L);
		hlsRepository.save(hlsPackage);

		mvc.perform(delete("/api/videos/{id}", saved.getId()))
				.andExpect(status().isNoContent());

		assertThat(repository.findById(saved.getId())).isEmpty();
		assertThat(hlsRepository.findById(saved.getId())).isEmpty();
		assertThat(Files.exists(storagePath)).isFalse();
		assertThat(Files.exists(segmentDir)).isFalse();
	}

	@Test
	void deleteShouldReturnNotFoundWhenVideoNotExists() throws Exception {
		mvc.perform(delete("/api/videos/{id}", 999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("VIDEO_NOT_FOUND"));
	}

	@Test
	void deleteShouldReturnInternalServerErrorWhenStorageFails() throws Exception {
		Video video = new Video();
		video.setTitle("ToDelete");
		video.setOriginalFilename("delete.mp4");
		video.setSizeBytes(12L);
		video.setStatus(VideoStatus.READY);
		Path storagePath = BASE_DIR.resolve("videos").resolve("11").resolve("delete.mp4");
		Files.createDirectories(storagePath.getParent());
		Files.write(storagePath, validMp4Bytes());
		video.setStoragePath(storagePath.toString());
		video.setCreatedAt(123L);
		video.setUpdatedAt(123L);
		Video saved = repository.save(video);

		HlsPackage hlsPackage = new HlsPackage();
		hlsPackage.setVideoId(saved.getId());
		hlsPackage.setManifestPath(storagePath.getParent().resolve("hls").resolve("index.m3u8").toString());
		Path segmentDir = storagePath.getParent().resolve("hls");
		Files.createDirectories(segmentDir);
		hlsPackage.setSegmentDir(segmentDir.toString());
		hlsPackage.setSegmentPattern("seg_%05d.ts");
		hlsPackage.setSegmentDurationSeconds(2);
		hlsPackage.setSegmentCount(1);
		hlsPackage.setTotalDurationSeconds(2);
		hlsPackage.setGeneratedAt(123L);
		hlsRepository.save(hlsPackage);

		Files.delete(storagePath);

		mvc.perform(delete("/api/videos/{id}", saved.getId()))
				.andExpect(status().isNoContent());

		assertThat(repository.findById(saved.getId())).isEmpty();
		assertThat(hlsRepository.findById(saved.getId())).isEmpty();
	}

	@Test
	void deleteShouldSucceedWhenHlsPackageMissing() throws Exception {
		Video video = new Video();
		video.setTitle("ToDelete");
		video.setOriginalFilename("delete.mp4");
		video.setSizeBytes(12L);
		video.setStatus(VideoStatus.UPLOADING);
		Path storagePath = BASE_DIR.resolve("videos").resolve("12").resolve("delete.mp4");
		Files.createDirectories(storagePath.getParent());
		Files.write(storagePath, validMp4Bytes());
		video.setStoragePath(storagePath.toString());
		video.setCreatedAt(123L);
		video.setUpdatedAt(123L);
		Video saved = repository.save(video);

		mvc.perform(delete("/api/videos/{id}", saved.getId()))
				.andExpect(status().isNoContent());

		assertThat(repository.findById(saved.getId())).isEmpty();
		assertThat(Files.exists(storagePath)).isFalse();
	}

	@Test
	void deleteShouldCascadeDeleteHlsPackage() throws Exception {
		Video video = new Video();
		video.setTitle("ToDelete");
		video.setOriginalFilename("delete.mp4");
		video.setSizeBytes(12L);
		video.setStatus(VideoStatus.READY);
		Path storagePath = BASE_DIR.resolve("videos").resolve("13").resolve("delete.mp4");
		Files.createDirectories(storagePath.getParent());
		Files.write(storagePath, validMp4Bytes());
		video.setStoragePath(storagePath.toString());
		video.setCreatedAt(123L);
		video.setUpdatedAt(123L);
		Video saved = repository.save(video);

		HlsPackage hlsPackage = new HlsPackage();
		hlsPackage.setVideoId(saved.getId());
		hlsPackage.setManifestPath(storagePath.getParent().resolve("hls").resolve("index.m3u8").toString());
		Path segmentDir = storagePath.getParent().resolve("hls");
		Files.createDirectories(segmentDir);
		hlsPackage.setSegmentDir(segmentDir.toString());
		hlsPackage.setSegmentPattern("seg_%05d.ts");
		hlsPackage.setSegmentDurationSeconds(2);
		hlsPackage.setSegmentCount(1);
		hlsPackage.setTotalDurationSeconds(2);
		hlsPackage.setGeneratedAt(123L);
		hlsRepository.save(hlsPackage);

		mvc.perform(delete("/api/videos/{id}", saved.getId()))
				.andExpect(status().isNoContent());

		assertThat(repository.findById(saved.getId())).isEmpty();
		assertThat(hlsRepository.findById(saved.getId())).isEmpty();
	}

	private static byte[] validMp4Bytes() {
		return new byte[] { 0, 0, 0, 18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm' };
	}
}
