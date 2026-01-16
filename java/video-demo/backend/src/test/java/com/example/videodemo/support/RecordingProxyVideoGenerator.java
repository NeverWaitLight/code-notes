package com.example.videodemo.support;

import com.example.videodemo.service.ProxyVideoGenerator;
import com.example.videodemo.service.ProxyVideoResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class RecordingProxyVideoGenerator implements ProxyVideoGenerator {

	private final AtomicInteger callCount = new AtomicInteger();
	private final AtomicReference<Path> lastInputPath = new AtomicReference<>();
	private final AtomicReference<Path> lastOutputPath = new AtomicReference<>();
	private boolean shouldFail = false;

	@Override
	public ProxyVideoResult generate(Path inputPath, Path outputPath) throws Exception {
		callCount.incrementAndGet();
		lastInputPath.set(inputPath);
		lastOutputPath.set(outputPath);

		if (shouldFail) {
			throw new RuntimeException("Proxy video generation failed (test)");
		}

		// 创建模拟的代理视频文件
		Files.createDirectories(outputPath.getParent());
		Files.write(outputPath, new byte[] { 0, 1, 2, 3 }); // 模拟更小的文件

		return new ProxyVideoResult(outputPath, 4L, 100, 10.0);
	}

	public int getCallCount() {
		return callCount.get();
	}

	public Path getLastInputPath() {
		return lastInputPath.get();
	}

	public Path getLastOutputPath() {
		return lastOutputPath.get();
	}

	public void setShouldFail(boolean shouldFail) {
		this.shouldFail = shouldFail;
	}

	public void reset() {
		callCount.set(0);
		lastInputPath.set(null);
		lastOutputPath.set(null);
		shouldFail = false;
	}
}
