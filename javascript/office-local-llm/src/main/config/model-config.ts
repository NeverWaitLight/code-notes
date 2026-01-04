// 模型配置文件
// 定义模型配置接口
export interface ModelConfig {
  id: string // 模型唯一标识
  name: string // 模型显示名称
  filename: string // 模型文件名
  url: string // 模型下载URL
  size: string // 模型大小显示
  sizeBytes: number // 模型大小（字节）
  description?: string // 模型描述（可选）
}

// 可用模型列表
export const AVAILABLE_MODELS: ModelConfig[] = [
  {
    id: 'qwen3-4b',
    name: 'Qwen3-4B-Q4_K_M',
    filename: 'Qwen3-4B-Q4_K_M.gguf',
    url: 'http://192.168.101.67/Qwen3-4B-Q4_K_M.gguf',
    size: '约2.3G',
    sizeBytes: 2300000000,
    description: '通义千问4B量化版本'
  },
  {
    id: 'qwen3-8b',
    name: 'Qwen3-8B-Q8_0',
    filename: 'Qwen3-8B-Q8_0.gguf',
    url: 'http://192.168.101.67/Qwen3-8B-Q8_0.gguf',
    size: '约8.5G',
    sizeBytes: 8500000000,
    description: '通义千问8B量化版本'
  },
  {
    id: 'qwen2.5-7b',
    name: 'Axi_Qwen2.5-7B-Instruct',
    filename: 'Axi_Qwen2.5-7B-Instruct.gguf',
    url: 'http://192.168.101.67/Axi_Qwen2.5-7B-Instruct.gguf',
    size: '约14G',
    sizeBytes: 14000000000,
    description: '通义千问2.5代7B指令版'
  }
]

// 获取默认模型
export const getDefaultModel = (): ModelConfig => {
  return AVAILABLE_MODELS[0] // 默认使用Qwen3-4B
}

// 根据ID获取模型
export const getModelById = (id: string): ModelConfig | undefined => {
  return AVAILABLE_MODELS.find((model) => model.id === id)
}
