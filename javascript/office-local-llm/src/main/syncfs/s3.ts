import {
  S3Client,
  CreateBucketCommand,
  ListObjectsV2Command,
  HeadBucketCommand,
  PutObjectCommand,
  DeleteObjectCommand,
  HeadObjectCommand,
  GetObjectCommand,
  _Object
} from '@aws-sdk/client-s3'
import log from 'electron-log'
import fs from 'fs'
import path from 'path'

// S3服务类
export class S3Service {
  // 桶名称
  static readonly BUCKET_NAME = '9527'

  // S3客户端配置
  private static readonly s3Config = {
    endpoint: 'http://localhost:8333', // 本地S3服务端点
    credentials: {
      accessKeyId: 'minioadmin', // 默认访问密钥，按需修改
      secretAccessKey: 'minioadmin' // 默认密钥，按需修改
    },
    region: 'us-east-1', // 默认区域
    forcePathStyle: true // 使用路径样式寻址
  }

  // 创建S3客户端
  private static readonly s3Client = new S3Client(S3Service.s3Config)

  /**
   * 检查桶是否存在
   * @param bucketName 桶名
   */
  private static async bucketExists(bucketName: string): Promise<boolean> {
    try {
      const command = new HeadBucketCommand({ Bucket: bucketName })
      await S3Service.s3Client.send(command)
      return true
    } catch (error) {
      const s3Error = error as { name?: string; $metadata?: { httpStatusCode?: number } }
      if (s3Error.name === 'NotFound' || s3Error.$metadata?.httpStatusCode === 404) {
        return false
      }
      // 其他错误重新抛出
      throw error
    }
  }

  /**
   * 创建桶
   * @param bucketName 桶名
   */
  private static async createBucket(bucketName: string): Promise<boolean> {
    try {
      const command = new CreateBucketCommand({ Bucket: bucketName })
      await S3Service.s3Client.send(command)
      log.info(`S3桶创建成功: ${bucketName}`)
      return true
    } catch (error) {
      log.error(`S3桶创建失败: ${bucketName}`, error)
      return false
    }
  }

  /**
   * 检查文件是否可访问(未被占用)
   */
  private static isFileAccessible(filePath: string): boolean {
    try {
      // 尝试以读写方式打开文件，如果文件被占用，这里会抛出异常
      const fd = fs.openSync(filePath, 'r+')
      fs.closeSync(fd)
      return true
    } catch {
      // 文件被占用或其他错误，返回false
      return false
    }
  }

  /**
   * 列出桶中的文件
   * @param bucketName 桶名
   */
  static async listFiles(bucketName: string): Promise<_Object[]> {
    try {
      const command = new ListObjectsV2Command({ Bucket: bucketName })
      const response = await S3Service.s3Client.send(command)

      if (response.Contents) {
        return response.Contents
      }

      return []
    } catch (error) {
      log.error(`列出S3桶文件失败: ${bucketName}`, error)
      return []
    }
  }

  /**
   * 获取S3对象元数据
   * @param key 对象键
   * @returns 对象元数据
   */
  static async getObjectMetadata(
    key: string
  ): Promise<{ LastModified?: Date; ETag?: string; XOriginalMtime?: Date | null } | null> {
    try {
      const command = new HeadObjectCommand({
        Bucket: S3Service.BUCKET_NAME,
        Key: key
      })

      const response = await S3Service.s3Client.send(command)
      return {
        LastModified: response.LastModified,
        ETag: response.ETag,
        XOriginalMtime: response.Metadata?.['x-original-mtime']
          ? new Date(response.Metadata['x-original-mtime'])
          : null
      }
    } catch (error) {
      const s3Error = error as { name?: string; $metadata?: { httpStatusCode?: number } }
      if (s3Error.name === 'NotFound' || s3Error.$metadata?.httpStatusCode === 404) {
        return null // 对象不存在
      }
      log.error(`获取S3对象元数据失败: ${key}`, error)
      throw error // 重新抛出其他错误
    }
  }

  /**
   * 从S3下载文件
   */
  static async downloadFile(key: string, localFilePath: string): Promise<boolean> {
    try {
      // 确保目录存在
      const dir = path.dirname(localFilePath)
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true })
      }

      const command = new GetObjectCommand({
        Bucket: S3Service.BUCKET_NAME,
        Key: key
      })

      const response = await S3Service.s3Client.send(command)

      if (response.Body) {
        // 直接将内容写入文件
        const bodyContents = await response.Body.transformToByteArray()
        await fs.promises.writeFile(localFilePath, Buffer.from(bodyContents))
        log.info(`文件从S3下载成功: ${key} -> ${localFilePath}`)
        return true
      } else {
        throw new Error('响应体为空')
      }
    } catch (error) {
      log.error(`从S3下载文件失败: ${key}`, error)
      return false
    }
  }

  /**
   * 初始化S3服务
   */
  static async init(): Promise<void> {
    try {
      log.info(`正在连接S3服务: ${S3Service.s3Config.endpoint}`)

      // 检查桶是否存在
      const exists = await S3Service.bucketExists(S3Service.BUCKET_NAME)

      // 如果桶不存在，创建桶
      if (!exists) {
        log.info(`桶不存在: ${S3Service.BUCKET_NAME}, 正在创建...`)
        await S3Service.createBucket(S3Service.BUCKET_NAME)
      } else {
        log.info(`桶已存在: ${S3Service.BUCKET_NAME}`)
      }
    } catch (error) {
      log.error('S3服务初始化失败:', error)
    }
  }

  /**
   * 上传文件到S3桶，使用"最后修改者优先"策略
   * @param localFilePath 本地文件路径
   * @param relativePath 相对路径
   */
  static async uploadFile(localFilePath: string, relativePath: string): Promise<boolean> {
    // 检查文件是否可访问(未被占用)
    if (!S3Service.isFileAccessible(localFilePath)) {
      log.warn(`文件正在被占用，跳过上传: ${localFilePath}`)
      return false
    }

    // 在S3中使用相对路径作为键，去掉开头的斜杠
    const key = relativePath.startsWith('/') ? relativePath.substring(1) : relativePath

    // 获取本地文件状态
    const localStat = fs.statSync(localFilePath)
    const localMtime = localStat.mtime

    // 获取S3对象元数据(如果存在)
    const metadata = await S3Service.getObjectMetadata(key)
    if (metadata && metadata.XOriginalMtime) {
      // 如果S3对象存在，比较修改时间
      if (localMtime <= metadata.XOriginalMtime) {
        log.info(`S3版本更新或相同，跳过上传: ${key}`)
        return false
      }
      log.info(`本地文件更新，准备上传: ${key}`)
    }

    try {
      // 上传文件到S3
      const command = new PutObjectCommand({
        Bucket: S3Service.BUCKET_NAME,
        Key: key,
        Body: await fs.promises.readFile(localFilePath),
        Metadata: {
          'x-original-mtime': localMtime.toISOString()
        }
      })

      await S3Service.s3Client.send(command)
      log.info(`文件已上传到S3: ${key}`)
      return true
    } catch (error) {
      log.error(`上传文件到S3失败: ${localFilePath}`, error)
      return false
    }
  }

  /**
   * 从S3桶删除文件
   */
  static async deleteFile(relativePath: string): Promise<boolean> {
    try {
      // 在S3中使用相对路径作为键，去掉开头的斜杠
      const key = relativePath.startsWith('/') ? relativePath.substring(1) : relativePath

      // 从S3删除文件
      const command = new DeleteObjectCommand({
        Bucket: S3Service.BUCKET_NAME,
        Key: key
      })

      await S3Service.s3Client.send(command)
      log.info(`文件已从S3删除: ${key}`)
      return true
    } catch (error) {
      log.error(`从S3删除文件失败: ${relativePath}`, error)
      return false
    }
  }

  /**
   * 关闭S3服务
   */
  static close(): void {
    try {
      // 关闭客户端连接或执行清理工作
      log.info('S3服务已关闭')
    } catch (error) {
      log.error('关闭S3服务失败:', error)
    }
  }
}
