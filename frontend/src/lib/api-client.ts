import { getAccessToken } from './auth/keycloak'

// Thin fetch wrapper: same-origin in production (SPA served by Spring Boot under /app/**,
// so the JSESSIONID cookie for the app-level questionnaire session rides along automatically),
// dev-proxied to :8080 by Vite (see vite.config.ts). Anonymous calls (questionnaire, results,
// health-centers) simply have no token attached — those endpoints are public in SecurityConfig.
// Mirrors I18nProvider's stored language choice so the backend serves translated question
// content (DecisionService reads the request's Accept-Language). Read lazily per request —
// the user can switch language mid-questionnaire.
function currentLanguage(): string {
  if (typeof window === 'undefined') return 'en'
  return window.localStorage.getItem('afyacheck-lang') ?? 'en'
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = await getAccessToken()

  const response = await fetch(path, {
    ...init,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      'Accept-Language': currentLanguage(),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error((body && body.error) || `Request to ${path} failed: ${response.status}`)
  }

  return response.json() as Promise<T>
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return apiFetch<T>(path, { method: 'POST', body: JSON.stringify(body) })
}

export function apiDelete<T>(path: string): Promise<T> {
  return apiFetch<T>(path, { method: 'DELETE' })
}
