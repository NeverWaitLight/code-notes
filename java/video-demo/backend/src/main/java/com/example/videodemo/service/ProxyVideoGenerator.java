package com.example.videodemo.service;

import java.nio.file.Path;

public interface ProxyVideoGenerator {

	ProxyVideoResult generate(Path inputPath, Path outputPath) throws Exception;
}
