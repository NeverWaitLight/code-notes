package com.example.videodemo.service;

import java.nio.file.Path;

public record HlsSegmentResult(Path manifestPath, int segmentCount, int totalDurationSeconds) {
}
