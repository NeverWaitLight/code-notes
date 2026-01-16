package com.example.videodemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private String storageRoot;
	private String hlsRoot;
	private String dbPath;
	private long uploadMaxBytes;
	private int hlsSegmentSeconds;

	public String getStorageRoot() {
		return storageRoot;
	}

	public void setStorageRoot(String storageRoot) {
		this.storageRoot = storageRoot;
	}

	public String getHlsRoot() {
		return hlsRoot;
	}

	public void setHlsRoot(String hlsRoot) {
		this.hlsRoot = hlsRoot;
	}

	public String getDbPath() {
		return dbPath;
	}

	public void setDbPath(String dbPath) {
		this.dbPath = dbPath;
	}

	public long getUploadMaxBytes() {
		return uploadMaxBytes;
	}

	public void setUploadMaxBytes(long uploadMaxBytes) {
		this.uploadMaxBytes = uploadMaxBytes;
	}

	public int getHlsSegmentSeconds() {
		return hlsSegmentSeconds;
	}

	public void setHlsSegmentSeconds(int hlsSegmentSeconds) {
		this.hlsSegmentSeconds = hlsSegmentSeconds;
	}
}
