import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { getAccessToken, setAccessToken } from './tokenStore'
import type { AuthResponse } from './types'

// Same origin in production (the SPA is served by Spring Boot itself);
// in dev, Vite's server.proxy makes /api same-origin from the browser's
// point of view too, so a relative base URL works in both cases.
export const apiClient = axios.create({
  baseURL: '/api',
  withCredentials: true, // send the httpOnly refresh cookie on refresh calls
})

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retried?: boolean
}

let refreshPromise: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  // Coalesce concurrent 401s into a single refresh call instead of firing
  // one per failed request.
  if (!refreshPromise) {
    refreshPromise = apiClient
      .post<AuthResponse>('/auth/refresh')
      .then((response) => {
        setAccessToken(response.data.accessToken)
        return response.data.accessToken
      })
      .catch(() => {
        setAccessToken(null)
        return null
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetriableRequestConfig | undefined

    const isAuthEndpoint = config?.url?.startsWith('/auth/')
    if (error.response?.status === 401 && config && !config._retried && !isAuthEndpoint) {
      config._retried = true
      const newToken = await refreshAccessToken()
      if (newToken) {
        config.headers.Authorization = `Bearer ${newToken}`
        return apiClient(config)
      }
    }

    return Promise.reject(error)
  },
)

export { refreshAccessToken }
