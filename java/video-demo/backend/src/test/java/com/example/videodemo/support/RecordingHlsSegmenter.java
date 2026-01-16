package com.example.videodemo.support;

import com.example.videodemo.service.HlsSegmentResult;
import com.example.videodemo.service.HlsSegmenter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RecordingHlsSegmenter implements HlsSegmenter {

	private final AtomicInteger callCount = new AtomicInteger();
	private final AtomicInteger lastSegmentSeconds = new AtomicInteger();
	private final AtomicReference<Path> lastInput = new AtomicReference<>();
	private final AtomicReference<Path> lastOutputDir = new AtomicReference<>();
	private final AtomicReference<String> lastSegmentPattern = new AtomicReference<>();

	@Override
	public HlsSegmentResult segment(Path input, Path outputDir, int segmentDurationSeconds, String segmentPattern)
			throws Exception {
		callCount.incrementAndGet();
		lastSegmentSeconds.set(segmentDurationSeconds);
		lastInput.set(input);
		lastOutputDir.set(outputDir);
		lastSegmentPattern.set(segmentPattern);

		Files.createDirectories(outputDir);
		Path manifestPath = outputDir.resolve("index.m3u8");
		Files.writeString(manifestPath, "#EXTM3U\n");
		String segmentName = String.format(segmentPattern, 0);
		Files.write(outputDir.resolve(segmentName), new byte[] { 0 });
		return new HlsSegmentResult(manifestPath, 1, segmentDurationSeconds);
	}

	public int getCallCount() {
		return callCount.get();
	}

	public int getLastSegmentSeconds() {
		return lastSegmentSeconds.get();
	}

	public Path getLastInput() {
		return lastInput.get();
	}

	public Path getLastOutputDir() {
		return lastOutputDir.get();
	}

	public String getLastSegmentPattern() {
		return lastSegmentPattern.get();
	}

	public void reset() {
		callCount.set(0);
		lastSegmentSeconds.set(0);
		lastInput.set(null);
		lastOutputDir.set(null);
		lastSegmentPattern.set(null);
	}
}
