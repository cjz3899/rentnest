import http, { type ApiResult } from './http'

export interface SessionData {
  token: string
  refreshToken: string
  userId: number
  nickname: string
  role: string
}

export interface ProfileData {
  id: number
  phone: string
  nickname: string
  avatarFileId: number | null
  role: string
  realName: string | null
  email: string | null
  createdAt: string
}

export const authApi = {
  login(phone: string, password: string) {
    return http.post<ApiResult<SessionData>>('/auth/login', { phone, password })
  },
  refresh(refreshToken: string) {
    return http.post<ApiResult<SessionData>>('/auth/refresh', { refreshToken })
  },
  logout(refreshToken: string) {
    return http.post<ApiResult<void>>('/auth/logout', { refreshToken })
  },
}

export const userApi = {
  profile() {
    return http.get<ApiResult<ProfileData>>('/user/profile')
  },
}
