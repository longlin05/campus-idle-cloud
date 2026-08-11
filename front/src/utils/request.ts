import axios from 'axios'
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

export interface Result<T = any> {
  code: number
  message: string
  data: T
  msg?: string
}

/** 请求超时分级（毫秒） */
const TIMEOUT = {
  QUERY: 8000,       // 查询类接口：8s
  MUTATION: 10000,   // 下单/支付/状态变更：10s
  UPLOAD: 30000,     // 文件上传：30s
}

/** 根据请求方法和 URL 自动选择超时时间 */
function resolveTimeout(config: AxiosRequestConfig): number {
  const url = config.url || ''
  // 文件上传
  if (url.includes('/upload') || url.includes('/avatar') || (config.headers?.['Content-Type'] as string)?.includes('multipart')) {
    return TIMEOUT.UPLOAD
  }
  // 写操作
  if (['post', 'put', 'delete', 'patch'].includes((config.method || 'get').toLowerCase())) {
    return TIMEOUT.MUTATION
  }
  // 查询操作
  return TIMEOUT.QUERY
}

const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: TIMEOUT.QUERY,
  headers: {
    'Content-Type': 'application/json',
  },
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 根据请求类型动态设置超时
    config.timeout = resolveTimeout(config)

    const isAdminApi = (config.url || '').startsWith('/admin')
    const userToken = localStorage.getItem('token')
    const adminToken = localStorage.getItem('admin_token')
    const token = isAdminApi ? (adminToken || userToken) : (userToken || adminToken)
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const res = response.data
    if (res && res.code !== undefined && res.code !== 200) {
      if (res.code === 401) {
        const isAdmin = location.pathname.startsWith('/admin')
        if (isAdmin) {
          localStorage.removeItem('admin_token')
          localStorage.removeItem('adminInfo')
          location.href = '/admin/login'
        } else {
          localStorage.removeItem('token')
          localStorage.removeItem('userInfo')
          location.href = '/login'
        }
      }
      const err = new Error(res.message || res.msg || 'Error')
      ;(err as any).code = res.code
      return Promise.reject(err)
    }
    return response
  },
  (error) => {
    // 打印详细错误到控制台，方便排查
    console.error('[API Error]', error.config?.method?.toUpperCase(), error.config?.url, error)

    if (error.response?.status === 401) {
      const isAdmin = location.pathname.startsWith('/admin')
      if (isAdmin) {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('adminInfo')
        location.href = '/admin/login'
      } else {
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        location.href = '/login'
      }
    }

    // 组装更友好的错误信息
    let message = '请求失败'
    if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      message = '请求超时，后端服务可能未启动或响应过慢'
    } else if (error.code === 'ERR_NETWORK' || !error.response) {
      message = '网络错误，请检查网关(campus-gateway:8080)和对应微服务是否已启动'
    } else if (error.response) {
      message = `服务器错误 ${error.response.status}: ${error.response.statusText || '未知错误'}`
    }
    const wrappedError = new Error(message)
    ;(wrappedError as any).original = error
    return Promise.reject(wrappedError)
  }
)

export default service
