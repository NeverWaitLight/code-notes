import type { ApiErrorResponse } from '../types/api'

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  try {
    const res = await fetch(`${API_BASE}${path}`, init)
    
    if (res.status === 204) {
      return undefined as T
    }
    
    let data: any
    const contentType = res.headers.get('content-type')
    if (contentType && contentType.includes('application/json')) {
      data = await res.json()
    } else {
      const text = await res.text()
      data = text ? { error: { code: 'INVALID_RESPONSE', message: text } } : { error: { code: 'INVALID_RESPONSE', message: '服务器返回了非 JSON 格式的响应' } }
    }
    
    if (!res.ok) {
      const errorResponse = data as ApiErrorResponse
      throw { error: errorResponse?.error ?? { code: 'UNKNOWN', message: '请求失败' } }
    }
    
    return data as T
  } catch (err: any) {
    if (err?.error) {
      throw err
    }
    throw { error: { code: 'NETWORK_ERROR', message: err?.message || '网络请求失败' } }
  }
}

export const apiClient = { request }
