package com.example.videodemo.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.springframework.stereotype.Component;

@Component
public class JavaCvHlsSegmenter implements HlsSegmenter {

	private static final String MANIFEST_FILENAME = "index.m3u8";

	@Override
	public HlsSegmentResult segment(Path input, Path outputDir, int segmentDurationSeconds, String segmentPattern)
			throws Exception {
		Files.createDirectories(outputDir);
		Path manifestPath = outputDir.resolve(MANIFEST_FILENAME);
		String segmentFilename = outputDir.resolve(segmentPattern).toAbsolutePath().toString();

		// 获取视频时长（仍使用 JavaCV）
		long lengthMicros;
		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(input.toString())) {
			grabber.start();
			lengthMicros = grabber.getLengthInTime();
			grabber.stop();
		}

		// 使用 JavaCV 内置的 FFmpeg 可执行文件进行 HLS 切片
		String ffmpegPath = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);

		ProcessBuilder pb = new ProcessBuilder(
				ffmpegPath,
				"-i", input.toAbsolutePath().toString(),
				"-c", "copy", // 流复制，不重新编码
				"-f", "hls",
				"-hls_time", String.valueOf(segmentDurationSeconds),
				"-hls_list_size", "0",
				"-hls_segment_filename", segmentFilename,
				"-start_number", "0",
				"-hls_flags", "independent_segments",
				"-y", // 覆盖已存在的文件
				manifestPath.toAbsolutePath().toString());
		pb.redirectErrorStream(true);

		Process process = pb.start();

		// 捕获输出日志
		StringBuilder output = new StringBuilder();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line).append("\n");
			}
		}

		boolean finished = process.waitFor(5, TimeUnit.MINUTES);

		if (!finished) {
			process.destroyForcibly();
			throw new IOException("FFmpeg process timeout after 5 minutes");
		}

		int exitCode = process.exitValue();
		if (exitCode != 0) {
			throw new IOException("FFmpeg process failed with exit code: " + exitCode + "\nOutput:\n" + output);
		}

		if (!Files.exists(manifestPath)) {
			throw new IllegalStateException("HLS manifest not generated");
		}

		int segmentCount;
		try (var stream = Files.list(outputDir)) {
			segmentCount = (int) stream.filter(path -> path.getFileName().toString().endsWith(".ts")).count();
		}
		int totalDurationSeconds = (int) Math.max(0, Math.ceil(lengthMicros / 1_000_000.0));
		return new HlsSegmentResult(manifestPath, segmentCount, totalDurationSeconds);
	}
}
