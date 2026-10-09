import { defineStore } from 'pinia'
import { authApi, type SessionData } from '@/api/auth'

const STORAGE_KEY = 'rentnest.session'

interface SessionState {
  accessToken: string
  refreshToken: string
  userId: number
  nickname: string
  role: string
}

function loadSession(): SessionState | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as SessionState) : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => {
    const s = loadSession()
    return {
      accessToken: s?.accessToken ?? null,
      refreshToken: s?.refreshToken ?? null,
      userId: s?.userId ?? null,
      nickname: s?.nickname ?? null,
      role: s?.role ?? null,
    }
  },
  getters: {
    isLoggedIn: (state) => !!state.accessToken && !!state.refreshToken,
  },
  actions: {
    setSession(data: SessionData) {
      this.accessToken = data.token
      this.refreshToken = data.refreshToken
      this.userId = data.userId
      this.nickname = data.nickname
      this.role = data.role
      const session: SessionState = {
        accessToken: data.token,
        refreshToken: data.refreshToken,
        userId: data.userId,
        nickname: data.nickname,
        role: data.role,
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    },
    clearSession() {
      this.accessToken = null
      this.refreshToken = null
      this.userId = null
      this.nickname = null
      this.role = null
      localStorage.removeItem(STORAGE_KEY)
    },
    async login(phone: string, password: string) {
      const resp = await authApi.login(phone, password)
      this.setSession(resp.data.data)
    },
    async refreshSession(): Promise<string | null> {
      if (!this.refreshToken) {
        return null
      }
      try {
        const resp = await authApi.refresh(this.refreshToken)
        this.setSession(resp.data.data)
        return this.accessToken
      } catch {
        this.clearSession()
        return null
      }
    },
    async logout() {
      const rt = this.refreshToken
      this.clearSession()
      if (rt) {
        try {
          await authApi.logout(rt)
        } catch {
        }
      }
    },
  },
})
