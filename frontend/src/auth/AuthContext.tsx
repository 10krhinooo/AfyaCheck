import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { apiClient, refreshAccessToken } from '../api/client'
import { setAccessToken } from '../api/tokenStore'
import type { AuthResponse, CurrentUser } from '../api/types'

interface AuthContextValue {
  user: CurrentUser | null
  isLoading: boolean
  isAdmin: boolean
  login: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
  refresh: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<CurrentUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const silentRefresh = useCallback(async () => {
    const token = await refreshAccessToken()
    if (token) {
      const response = await apiClient.get<CurrentUser>('/auth/me')
      setUser(response.data)
    } else {
      setUser(null)
    }
  }, [])

  useEffect(() => {
    silentRefresh().finally(() => setIsLoading(false))
    // Only run once, on mount: this is the app's initial "am I logged in"
    // check, not something that should re-run when silentRefresh's
    // identity happens to change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const response = await apiClient.post<AuthResponse>('/auth/login', { email, password })
    setAccessToken(response.data.accessToken)
    setUser(response.data.user)
  }, [])

  const logout = useCallback(async () => {
    await apiClient.post('/auth/logout')
    setAccessToken(null)
    setUser(null)
  }, [])

  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false

  return (
    <AuthContext.Provider value={{ user, isLoading, isAdmin, login, logout, refresh: silentRefresh }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
