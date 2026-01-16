# Data Models

## Video

**Purpose:** 表示上传视频的元数据与处理状态，用于列表展示、播放与删除。

**Key Attributes:**

- id: number - 视频唯一标识（自增）
- title: string - 用户输入标题
- originalFilename: string - 原始文件名
- sizeBytes: number - 文件大小
- status: "UPLOADING" | "READY" | "FAILED" - 处理状态
- errorCode: string? - 失败错误码（可空）
- errorMessage: string? - 失败错误信息（可空）
- storagePath: string - 原始文件存储路径
- createdAt: number - 创建时间（Unix ms）
- updatedAt: number - 状态更新时间（Unix ms）

### TypeScript Interface

```typescript
export interface Video {
  id: number;
  title: string;
  originalFilename: string;
  sizeBytes: number;
  status: "UPLOADING" | "READY" | "FAILED";
  errorCode?: string;
  errorMessage?: string;
  storagePath: string;
  createdAt: number;
  updatedAt: number;
}
```

### Relationships

- Video 通过 `videoId` 与 HlsPackage 建立 1:1 关联（仅当切片完成时存在）。
- 列表与详情接口以 Video 为主实体，按需聚合 HlsPackage 生成播放地址。
- 详情接口返回 `manifestUrl`（由 `manifestPath` 映射生成）。

## HlsPackage

**Purpose:** 管理逻辑视频与 HLS 切片产物的关系，记录清单与切片目录信息。

**Key Attributes:**

- videoId: number - 关联 Video.id（主键/外键）
- manifestPath: string - HLS 清单相对路径（如 `data/videos/{id}/hls/index.m3u8`）
- segmentDir: string - 切片目录（如 `data/videos/{id}/hls/`）
- segmentPattern: string - 切片文件命名模式（如 `seg_%05d.ts`）
- segmentDurationSeconds: number - 切片时长（固定 2）
- segmentCount: number - 切片数量
- totalDurationSeconds: number - 总时长（可选）
- generatedAt: number - 切片完成时间（Unix ms）

### TypeScript Interface

```typescript
export interface HlsPackage {
  videoId: number;
  manifestPath: string;
  segmentDir: string;
  segmentPattern: string;
  segmentDurationSeconds: number;
  segmentCount: number;
  totalDurationSeconds?: number;
  generatedAt: number;
}
```

### Relationships

- HlsPackage 属于 Video（1:1），切片文件列表以 `.m3u8` 清单为准，不在数据库中逐条存储。
