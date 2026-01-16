package com.example.videodemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class VideodemoApplicationTests {

	@Autowired
	private Environment environment;

	@Test
	void contextLoads() {
	}

	@Test
	void storageAndDatabasePathsExist() {
		Path baseDir = Paths.get("").toAbsolutePath();
		String storageRootValue = environment.getProperty("app.storage-root");
		String hlsRootValue = environment.getProperty("app.hls-root");
		String dbPathValue = environment.getProperty("app.db-path");
		assertNotNull(storageRootValue, "app.storage-root should be configured");
		assertNotNull(hlsRootValue, "app.hls-root should be configured");
		assertNotNull(dbPathValue, "app.db-path should be configured");
		Path storageRoot = resolvePath(baseDir, storageRootValue);
		Path hlsRoot = resolvePath(baseDir, hlsRootValue);
		Path dbPath = resolvePath(baseDir, dbPathValue);

		assertTrue(Files.isDirectory(storageRoot), "storage root directory should exist");
		assertTrue(Files.isDirectory(hlsRoot), "hls root directory should exist");
		assertTrue(Files.isRegularFile(dbPath), "database file should exist");
	}

	private Path resolvePath(Path baseDir, String configuredPath) {
		Path path = Paths.get(configuredPath);
		if (path.isAbsolute()) {
			return path.normalize();
		}
		return baseDir.resolve(path).normalize();
	}

}
