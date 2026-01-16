export type VideoStatus = 'UPLOADING' | 'READY' | 'FAILED'

export interface Video {
  id: number
  title: string
  status: VideoStatus
  sizeBytes: number
  createdAt: number
}

export type VideoDetail = Video & {
  manifestUrl: string
}
