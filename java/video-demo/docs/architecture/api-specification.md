# API Specification

## REST API Specification

```yaml
openapi: 3.0.0
info:
  title: video-demo-api
  version: "1.0.0"
  description: Local demo API for video upload, processing, playback, and deletion
servers:
  - url: http://localhost:8080
    description: Local Spring Boot server
```

**Endpoints 概览：**

- `POST /api/videos` 上传视频（multipart/form-data）
- `GET /api/videos` 获取视频列表
- `GET /api/videos/{id}` 获取视频详情（含 HLS 清单地址）
- `DELETE /api/videos/{id}` 删除视频及其产物
- `GET /media/{id}/index.m3u8` 获取 HLS 清单（静态资源或专用接口）

## DTO Definitions

```json
// VideoListItem
{
  "id": 1,
  "title": "示例视频",
  "status": "READY",
  "sizeBytes": 123456789,
  "createdAt": 1736840000000
}
```

```json
// VideoDetail
{
  "id": 1,
  "title": "示例视频",
  "status": "READY",
  "sizeBytes": 123456789,
  "createdAt": 1736840000000,
  "manifestUrl": "http://localhost:8080/media/1/index.m3u8"
}
```

```json
// ApiError
{
  "error": {
    "code": "VIDEO_NOT_FOUND",
    "message": "视频不存在",
    "timestamp": "2026-01-14T12:00:00Z",
    "requestId": "req-abc-123"
  }
}
```

## Endpoint Details

**POST /api/videos**  
上传视频（multipart/form-data）

Request (form-data):

- `file`: MP4 (H.264)
- `title`: string

Response 201:

```json
{
  "id": 1,
  "title": "示例视频",
  "status": "UPLOADING",
  "sizeBytes": 123456789,
  "createdAt": 1736840000000
}
```

Errors:

- 400 `INVALID_REQUEST`
- 400 `INVALID_MEDIA_TYPE`
- 413 `UPLOAD_TOO_LARGE`
- 500 `STORAGE_IO_ERROR`

**GET /api/videos**  
获取视频列表

Response 200:

```json
[
  {
    "id": 1,
    "title": "示例视频",
    "status": "READY",
    "sizeBytes": 123456789,
    "createdAt": 1736840000000
  }
]
```

**GET /api/videos/{id}**  
获取视频详情

Response 200:

```json
{
  "id": 1,
  "title": "示例视频",
  "status": "READY",
  "sizeBytes": 123456789,
  "createdAt": 1736840000000,
  "manifestUrl": "http://localhost:8080/media/1/index.m3u8"
}
```

Errors:

- 404 `VIDEO_NOT_FOUND`
- 409 `HLS_NOT_READY`

**DELETE /api/videos/{id}**  
删除视频

Response 204: no content

Errors:

- 404 `VIDEO_NOT_FOUND`
- 500 `STORAGE_IO_ERROR`

## Media URL Mapping

- `manifestUrl` 统一规则：`http://localhost:8080/media/{videoId}/index.m3u8`
- 文件落盘规则：`${APP_STORAGE_ROOT}/{videoId}/hls/index.m3u8`
  **选定方案：** 使用 Spring `ResourceHandler` 将 `/media/**` 映射到 `${APP_STORAGE_ROOT}/`（演示项目优先简化实现）。

```java
// WebMvcConfigurer (简化示例)
registry.addResourceHandler("/media/**")
        .addResourceLocations("file:" + appStorageRoot + "/");
```
