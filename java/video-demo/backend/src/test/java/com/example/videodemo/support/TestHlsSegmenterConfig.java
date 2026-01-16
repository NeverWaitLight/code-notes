package com.example.videodemo.support;

import com.example.videodemo.service.HlsSegmenter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestHlsSegmenterConfig {

	@Bean
	public RecordingHlsSegmenter recordingHlsSegmenter() {
		return new RecordingHlsSegmenter();
	}

	@Bean
	@Primary
	public HlsSegmenter hlsSegmenter(RecordingHlsSegmenter recordingHlsSegmenter) {
		return recordingHlsSegmenter;
	}

}
