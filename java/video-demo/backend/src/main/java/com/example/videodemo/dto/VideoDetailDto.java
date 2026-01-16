package com.example.videodemo.dto;

import com.example.videodemo.entity.VideoStatus;

public record VideoDetailDto(
		long id,
		String title,
		VideoStatus status,
		long sizeBytes,
		long createdAt,
		String manifestUrl) {
}
