import axios from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  requestId: string
  timestamp: string
}

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('oa_access_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  if (config.method && ['post', 'put', 'delete'].includes(config.method)) {
    config.headers['X-Idempotency-Key'] = crypto.randomUUID()
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body?.code && body.code !== 'SUCCESS') {
      const err = new Error(body.message || body.code) as Error & { code: string; requestId: string }
      err.code = body.code
      err.requestId = body.requestId
      return Promise.reject(err)
    }
    return body?.data ?? response.data
  },
  (error) => {
    // No response received (network error / timeout)
    if (!error.response) {
      if (error.code === 'ECONNABORTED') {
        ElMessage.error('请求超时，请稍后再试')
      } else {
        ElMessage.error('网络连接失败，请检查网络')
      }
      return Promise.reject(error)
    }

    const status = error.response.status

    if (status === 401) {
      localStorage.removeItem('oa_access_token')
      localStorage.removeItem('oa_user')
      window.location.href = '/login'
    } else if (status === 403) {
      ElMessage.error('无权限执行此操作')
    } else if (status === 409) {
      ElMessage.warning('重复请求，请勿重复提交')
    } else if (status === 429) {
      ElMessage.error('请求过于频繁，请稍后再试')
    } else if (status >= 500) {
      ElMessage.error('服务器错误，请稍后再试')
    }

    return Promise.reject(error)
  }
)
