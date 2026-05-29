import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  requestId: string
  timestamp: string
}

function generateUUID(): string {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

// 错误码 → 用户友好消息映射
const ERROR_MESSAGES: Record<string, string> = {
  // 认证
  USER_BAD_CREDENTIALS: '用户名或密码错误',
  USER_ACCOUNT_LOCKED: '账号已被锁定，请联系管理员',
  USER_PASSWORD_EXPIRED: '密码已过期，请修改密码',
  USER_2FA_REQUIRED: '请输入两步验证码',
  USER_2FA_INVALID: '验证码错误，请重试',
  TOKEN_EXPIRED: '登录已过期，请重新登录',
  TOKEN_INVALID: '登录凭证无效',
  // 组织
  DEPT_NOT_FOUND: '部门不存在',
  DEPT_HAS_CHILDREN: '该部门下有子部门，无法删除',
  DEPT_HAS_USERS: '该部门下有员工，无法删除',
  USER_NOT_FOUND: '用户不存在',
  USER_ALREADY_EXISTS: '用户已存在',
  // 权限
  ROLE_NOT_FOUND: '角色不存在',
  ROLE_IN_USE: '角色正在使用中，无法删除',
  PERMISSION_DENIED: '无权限访问',
  // OA
  LEAVE_BALANCE_INSUFFICIENT: '假期余额不足',
  // 工作流
  WF_INSTANCE_CANNOT_WITHDRAW: '当前状态无法撤回',
  WF_INSTANCE_CANNOT_TERMINATE: '当前状态无法终止',
  // 合同
  CONTRACT_EXPIRED: '合同已过期',
  CONTRACT_STATUS_INVALID: '合同状态不允许此操作',
  // 文件
  FILE_TOO_LARGE: '文件大小超过限制',
  FILE_TYPE_NOT_ALLOWED: '文件类型不支持',
  // 预算
  BUDGET_EXCEEDED: '预算已超支',
  // 通用
  VALIDATION_FAILED: '表单校验失败',
  CONFLICT: '数据冲突，请刷新后重试',
  TOO_MANY_REQUESTS: '请求过于频繁，请稍后再试',
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
    config.headers['X-Idempotency-Key'] = generateUUID()
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
    // 无响应（网络错误/超时）
    if (!error.response) {
      if (error.code === 'ECONNABORTED') {
        ElMessage.error('请求超时，请稍后再试')
      } else {
        ElMessage.error('网络连接失败，请检查网络')
      }
      return Promise.reject(error)
    }

    const status = error.response.status
    const data = error.response.data as ApiResponse<unknown> | undefined
    const serverCode = data?.code
    const serverMessage = data?.message

    // 优先使用服务端返回的错误码映射
    if (serverCode && ERROR_MESSAGES[serverCode]) {
      ElMessage.error(ERROR_MESSAGES[serverCode])
    } else if (status === 401) {
      localStorage.removeItem('oa_access_token')
      localStorage.removeItem('oa_user')
      window.location.href = '/login'
      return Promise.reject(error)
    } else if (status === 403) {
      if (!error.config?.url?.includes('/auth/login')) {
        ElMessage.error('无权限访问')
      } else {
        ElMessage.error('账号或密码错误')
      }
    } else if (status === 404) {
      ElMessage.warning('资源不存在')
    } else if (status === 409) {
      ElMessage.warning(serverMessage || '数据冲突，请刷新后重试')
    } else if (status === 429) {
      ElMessage.error('请求过于频繁，请稍后再试')
    } else if (status >= 500) {
      ElMessage.error(serverMessage || '服务器错误，请稍后再试')
    } else {
      ElMessage.error(serverMessage || '请求失败')
    }

    return Promise.reject(error)
  }
)
