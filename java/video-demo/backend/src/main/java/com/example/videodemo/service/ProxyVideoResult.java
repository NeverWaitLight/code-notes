package com.example.videodemo.service;

import java.nio.file.Path;

public record ProxyVideoResult(Path proxyPath, long fileSizeBytes, int frameCount, double durationSeconds) {
}
