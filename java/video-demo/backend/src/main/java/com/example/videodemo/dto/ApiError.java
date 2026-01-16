package com.example.videodemo.dto;

public record ApiError(ApiError.ErrorBody error) {

	public static ApiError of(String code, String message) {
		return new ApiError(new ErrorBody(code, message));
	}

	public record ErrorBody(String code, String message) {
	}
}
