import { getAccessToken, login } from './auth/keycloak'

// Carries the HTTP status so callers can tell "session is invalid, needs sign-in" (401) apart
// from a transient network/server failure worth a plain "try again".
export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

// Thin fetch wrapper: same-origin in production (SPA served by Spring Boot under /app/**,
// so the JSESSIONID cookie for the app-level questionnaire session rides along automatically),
// dev-proxied to :8080 by Vite (see vite.config.ts). Anonymous calls (questionnaire, results,
// health-centers) simply have no token attached, those endpoints are public in SecurityConfig.
// Mirrors I18nProvider's stored language choice so the backend serves translated question
// content (DecisionService reads the request's Accept-Language). Read lazily per request,
// the user can switch language mid-questionnaire.
function currentLanguage(): string {
  if (typeof window === 'undefined') return 'en'
  return window.localStorage.getItem('afyacheck-lang') ?? 'en'
}

// Spring's CookieCsrfTokenRepository (see SecurityConfig) writes the token into a JS-readable
// XSRF-TOKEN cookie; echoing it back as this header is the other half of the double-submit
// pattern. Read fresh per request since a page load's first GET is what sets the cookie.
function csrfToken(): string | null {
  if (typeof document === 'undefined') return null
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/)
  return match ? decodeURIComponent(match[1]) : null
}

const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE'])

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const token = await getAccessToken()
  const method = (init?.method ?? 'GET').toUpperCase()
  const xsrfToken = SAFE_METHODS.has(method) ? null : csrfToken()

  const response = await fetch(path, {
    ...init,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      'Accept-Language': currentLanguage(),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(xsrfToken ? { 'X-XSRF-TOKEN': xsrfToken } : {}),
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const message = (body && body.error) || `Request to ${path} failed: ${response.status}`
    if (response.status === 401) {
      // A 401 from our own backend on a protected endpoint means the session is invalid
      // (expired/revoked token, or oidc-client-ts's silent renew failed) even though the
      // client-side auth state still looked signed in. Send the user back through Keycloak
      // login instead of leaving them on a "try again" that would just 401 forever.
      login(window.location.pathname).catch(() => {})
    }
    throw new ApiError(response.status, message)
  }

  return response.json() as Promise<T>
}

export function apiPost<T>(path: string, body: unknown): Promise<T> {
  return apiFetch<T>(path, { method: 'POST', body: JSON.stringify(body) })
}

export function apiDelete<T>(path: string): Promise<T> {
  return apiFetch<T>(path, { method: 'DELETE' })
}
