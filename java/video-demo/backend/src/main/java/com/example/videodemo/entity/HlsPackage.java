package com.example.videodemo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "t_hls_packages")
public class HlsPackage {

	@Id
	@Column(name = "video_id")
	private Long videoId;

	@Column(name = "manifest_path", nullable = false)
	private String manifestPath;

	@Column(name = "segment_dir", nullable = false)
	private String segmentDir;

	@Column(name = "segment_pattern", nullable = false)
	private String segmentPattern;

	@Column(name = "segment_duration_seconds", nullable = false)
	private int segmentDurationSeconds;

	@Column(name = "segment_count", nullable = false)
	private int segmentCount;

	@Column(name = "total_duration_seconds", nullable = false)
	private int totalDurationSeconds;

	@Column(name = "generated_at", nullable = false)
	private long generatedAt;

	public Long getVideoId() {
		return videoId;
	}

	public void setVideoId(Long videoId) {
		this.videoId = videoId;
	}

	public String getManifestPath() {
		return manifestPath;
	}

	public void setManifestPath(String manifestPath) {
		this.manifestPath = manifestPath;
	}

	public String getSegmentDir() {
		return segmentDir;
	}

	public void setSegmentDir(String segmentDir) {
		this.segmentDir = segmentDir;
	}

	public String getSegmentPattern() {
		return segmentPattern;
	}

	public void setSegmentPattern(String segmentPattern) {
		this.segmentPattern = segmentPattern;
	}

	public int getSegmentDurationSeconds() {
		return segmentDurationSeconds;
	}

	public void setSegmentDurationSeconds(int segmentDurationSeconds) {
		this.segmentDurationSeconds = segmentDurationSeconds;
	}

	public int getSegmentCount() {
		return segmentCount;
	}

	public void setSegmentCount(int segmentCount) {
		this.segmentCount = segmentCount;
	}

	public int getTotalDurationSeconds() {
		return totalDurationSeconds;
	}

	public void setTotalDurationSeconds(int totalDurationSeconds) {
		this.totalDurationSeconds = totalDurationSeconds;
	}

	public long getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(long generatedAt) {
		this.generatedAt = generatedAt;
	}
}
