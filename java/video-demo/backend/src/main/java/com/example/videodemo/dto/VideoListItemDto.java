package com.example.videodemo.dto;

import com.example.videodemo.entity.VideoStatus;

public record VideoListItemDto(
		long id,
		String title,
		VideoStatus status,
		long sizeBytes,
		long createdAt) {
}
