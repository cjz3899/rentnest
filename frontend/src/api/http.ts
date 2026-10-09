import axios, { AxiosError, type AxiosRequestConfig } from 'axios'
import { useAuthStore } from '@/stores/auth'

export interface ApiResult<T> {
  code: number
  msg: string
  data: T
}

export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
  }
}

type RetriableConfig = AxiosRequestConfig & { _retried?: boolean }

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

let refreshing: Promise<string | null> | null = null

http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

http.interceptors.response.use(
  (response) => {
    const body = response.data
    if (body && typeof body === 'object' && 'code' in body && body.code !== 0) {
      return Promise.reject(new ApiError(body.code, body.msg))
    }
    return response
  },
  async (error: AxiosError<ApiResult<unknown>>) => {
    const original = error.config as RetriableConfig | undefined
    const auth = useAuthStore()
    const isAuthUrl = original?.url?.includes('/auth/')

    if (error.response?.status === 401 && original && !original._retried && !isAuthUrl) {
      original._retried = true
      if (!refreshing) {
        refreshing = auth.refreshSession().finally(() => {
          refreshing = null
        })
      }
      try {
        const token = await refreshing
        if (token) {
          original.headers = { ...original.headers, Authorization: `Bearer ${token}` }
          return http(original)
        }
      } catch {
        auth.clearSession()
      }
    }

    const body = error.response?.data
    if (body && typeof body === 'object' && 'code' in body) {
      return Promise.reject(new ApiError(body.code, body.msg))
    }
    return Promise.reject(new ApiError(-1, error.message))
  },
)

export default http
