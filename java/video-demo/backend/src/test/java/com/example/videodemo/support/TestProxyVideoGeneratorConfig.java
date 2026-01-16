package com.example.videodemo.support;

import com.example.videodemo.service.ProxyVideoGenerator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestProxyVideoGeneratorConfig {

	@Bean
	public RecordingProxyVideoGenerator recordingProxyVideoGenerator() {
		return new RecordingProxyVideoGenerator();
	}

	@Bean
	@Primary
	public ProxyVideoGenerator proxyVideoGenerator(RecordingProxyVideoGenerator recordingProxyVideoGenerator) {
		return recordingProxyVideoGenerator;
	}
}
