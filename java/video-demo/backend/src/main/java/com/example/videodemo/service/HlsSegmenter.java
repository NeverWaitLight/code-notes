package com.example.videodemo.service;

import java.nio.file.Path;

public interface HlsSegmenter {

	HlsSegmentResult segment(Path input, Path outputDir, int segmentDurationSeconds, String segmentPattern) throws Exception;
}
