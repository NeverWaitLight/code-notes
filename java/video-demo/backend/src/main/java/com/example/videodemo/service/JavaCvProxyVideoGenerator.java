package com.example.videodemo.service;

import com.example.videodemo.config.AppProperties;
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
public class JavaCvProxyVideoGenerator implements ProxyVideoGenerator {

	private final AppProperties properties;

	public JavaCvProxyVideoGenerator(AppProperties properties) {
		this.properties = properties;
	}

	@Override
	public ProxyVideoResult generate(Path inputPath, Path outputPath) throws Exception {
		if (!Files.exists(inputPath)) {
			throw new IllegalArgumentException("Input video file does not exist: " + inputPath);
		}

		// 获取原视频信息
		int originalFrameCount;
		double originalFrameRate;
		int originalBitrate;
		double durationSeconds;

		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputPath.toString())) {
			grabber.start();
			originalFrameCount = grabber.getLengthInFrames();
			originalFrameRate = grabber.getFrameRate();
			originalBitrate = grabber.getVideoBitrate();
			long lengthMicros = grabber.getLengthInTime();
			durationSeconds = lengthMicros / 1_000_000.0;
			grabber.stop();
		}

		// 计算目标码率（根据配置减少百分比）
		int reductionPercent = properties.getProxyVideoBitrateReductionPercent();
		if (reductionPercent <= 0 || reductionPercent >= 100) {
			reductionPercent = 50; // 默认 50%
		}
		int targetBitrate = (int) (originalBitrate * (1 - reductionPercent / 100.0));
		if (targetBitrate <= 0) {
			targetBitrate = 500_000; // 最低 500kbps
		}

		// 确保输出目录存在
		Files.createDirectories(outputPath.getParent());

		// 使用 FFmpeg 生成代理视频
		String ffmpegPath = Loader.load(org.bytedeco.ffmpeg.ffmpeg.class);

		// 使用 libopenh264 编码器（JavaCV FFmpeg 自带）
		ProcessBuilder pb = new ProcessBuilder(
				ffmpegPath,
				"-i", inputPath.toAbsolutePath().toString(),
				"-c:v", "libopenh264", // 使用 OpenH264 编码器
				"-b:v", String.valueOf(targetBitrate), // 目标视频码率
				"-r", String.valueOf(originalFrameRate), // 保持帧率不变
				"-c:a", "aac", // 音频编码
				"-b:a", "128k", // 音频码率
				"-movflags", "+faststart", // 优化 MP4 播放
				"-y", // 覆盖已存在的文件
				outputPath.toAbsolutePath().toString());
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

		boolean finished = process.waitFor(10, TimeUnit.MINUTES);

		if (!finished) {
			process.destroyForcibly();
			throw new IOException("FFmpeg proxy video generation timeout after 10 minutes");
		}

		int exitCode = process.exitValue();
		if (exitCode != 0) {
			throw new IOException(
					"FFmpeg proxy video generation failed with exit code: " + exitCode + "\nOutput:\n" + output);
		}

		if (!Files.exists(outputPath)) {
			throw new IllegalStateException("Proxy video file not generated: " + outputPath);
		}

		// 验证代理视频帧数与原视频一致
		int proxyFrameCount;
		try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(outputPath.toString())) {
			grabber.start();
			proxyFrameCount = grabber.getLengthInFrames();
			grabber.stop();
		}

		if (Math.abs(proxyFrameCount - originalFrameCount) > 1) {
			throw new IllegalStateException(
					"Proxy video frame count mismatch: expected " + originalFrameCount + ", got " + proxyFrameCount);
		}

		long fileSizeBytes = Files.size(outputPath);

		return new ProxyVideoResult(outputPath, fileSizeBytes, proxyFrameCount, durationSeconds);
	}
}
