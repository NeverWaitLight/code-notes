package com.example.videodemo.controller;

import com.example.videodemo.dto.VideoDetailDto;
import com.example.videodemo.dto.VideoListItemDto;
import com.example.videodemo.entity.Video;
import com.example.videodemo.service.VideoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

	private final VideoService service;

	public VideoController(VideoService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<VideoListItemDto> upload(@RequestParam("file") MultipartFile file,
			@RequestParam("title") String title) {
		Video video = service.upload(file, title);
		return ResponseEntity.status(HttpStatus.CREATED).body(toListItem(video));
	}

	@GetMapping
	public List<VideoListItemDto> list(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "50") int size) {
		return service.list(page, size).stream().map(VideoController::toListItem).toList();
	}

	@GetMapping("/{id}")
	public VideoDetailDto detail(@PathVariable("id") long id) {
		return service.getDetail(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") long id) {
		service.delete(id);
		return ResponseEntity.noContent().build();
	}

	private static VideoListItemDto toListItem(Video video) {
		return new VideoListItemDto(
				video.getId(),
				video.getTitle(),
				video.getStatus(),
				video.getSizeBytes(),
				video.getCreatedAt());
	}
}
