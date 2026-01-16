package com.example.videodemo.controller;

import com.example.videodemo.dto.ApiError;
import com.example.videodemo.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ApiError> handleApiException(ApiException ex) {
		String message = ex.getMessage() == null ? "Request failed" : ex.getMessage();
		return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getCode(), message));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiError.of("INVALID_REQUEST", "Missing required parameter"));
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	public ResponseEntity<ApiError> handleMissingPart(MissingServletRequestPartException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiError.of("INVALID_REQUEST", "Missing required parameter"));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(ApiError.of("UPLOAD_TOO_LARGE", "Upload exceeds size limit"));
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ApiError> handleMultipart(MultipartException ex) {
		Throwable cause = ex.getCause();
		if (cause instanceof MaxUploadSizeExceededException) {
			return handleMaxUpload((MaxUploadSizeExceededException) cause);
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(ApiError.of("INVALID_REQUEST", "Invalid multipart request"));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of("INTERNAL_ERROR", "Unexpected server error"));
	}
}
