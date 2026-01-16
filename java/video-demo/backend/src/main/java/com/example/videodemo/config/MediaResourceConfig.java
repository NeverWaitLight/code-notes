package com.example.videodemo.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

	private final AppProperties properties;

	public MediaResourceConfig(AppProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		Path storageRoot = resolveStorageRoot();
		String location = storageRoot.toUri().toString();
		if (!location.endsWith("/")) {
			location = location + "/";
		}
		registry.addResourceHandler("/media/**")
				.addResourceLocations(location)
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						return super.getResource(mapToHlsPath(resourcePath), location);
					}
				});
	}

	private Path resolveStorageRoot() {
		Path rootPath = Paths.get(properties.getStorageRoot());
		if (rootPath.isAbsolute()) {
			return rootPath.normalize();
		}
		return Paths.get("").toAbsolutePath().resolve(rootPath).normalize();
	}

	private String mapToHlsPath(String resourcePath) {
		if (resourcePath == null || resourcePath.isBlank()) {
			return resourcePath;
		}
		if (resourcePath.contains("/hls/")) {
			return resourcePath;
		}
		int slashIndex = resourcePath.indexOf('/');
		if (slashIndex < 0) {
			return resourcePath;
		}
		return resourcePath.substring(0, slashIndex)
				+ "/hls/"
				+ resourcePath.substring(slashIndex + 1);
	}
}
