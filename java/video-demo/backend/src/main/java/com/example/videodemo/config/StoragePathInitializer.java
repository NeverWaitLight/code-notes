package com.example.videodemo.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StoragePathInitializer implements BeanFactoryPostProcessor, EnvironmentAware {

	private Environment environment;

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		String storageRoot = environment.getProperty("app.storage-root", "./data/videos");
		String hlsRoot = environment.getProperty("app.hls-root", storageRoot);
		String dbPath = environment.getProperty("app.db-path", "./data/app.db");
		String multipartLocation = environment.getProperty("spring.servlet.multipart.location");
		try {
			ensurePaths(storageRoot, hlsRoot, dbPath, multipartLocation);
		} catch (IOException ex) {
			throw new IllegalStateException("Failed to initialize storage paths", ex);
		}
	}

	private void ensurePaths(String storageRootValue, String hlsRootValue, String dbPathValue, String multipartLocationValue)
			throws IOException {
		Path storageRoot = resolvePath(storageRootValue);
		Files.createDirectories(storageRoot);

		Path hlsRoot = resolvePath(hlsRootValue);
		Files.createDirectories(hlsRoot);

		Path dbPath = resolvePath(dbPathValue);
		Path parent = dbPath.getParent();
		if (parent != null) {
			Files.createDirectories(parent);
		}
		if (Files.notExists(dbPath)) {
			Files.createFile(dbPath);
		}

		if (multipartLocationValue != null && !multipartLocationValue.isBlank()) {
			Path multipartLocation = resolvePath(multipartLocationValue);
			Files.createDirectories(multipartLocation);
		}
	}

	private Path resolvePath(String configuredPath) {
		Path path = Paths.get(configuredPath);
		if (path.isAbsolute()) {
			return path.normalize();
		}

		Path baseDir = Paths.get("").toAbsolutePath();
		return baseDir.resolve(path).normalize();
	}
}
