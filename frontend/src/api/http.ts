import axios from 'axios'

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
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body?.code && body.code !== 'SUCCESS') {
      return Promise.reject(new Error(body.message || body.code))
    }
    return body?.data ?? response.data
  },
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('oa_access_token')
      localStorage.removeItem('oa_user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)
