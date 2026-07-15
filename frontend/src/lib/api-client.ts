// Thin fetch wrapper: same-origin in production (SPA served by Spring Boot under /app/**,
// so the JSESSIONID cookie for the app-level questionnaire session rides along automatically),
// dev-proxied to :8080 by Vite (see vite.config.ts). JWT attachment is wired up in Phase 7
// once Keycloak (auth/keycloak.ts) is integrated.
export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, {
    ...init,
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    throw new Error(`Request to ${path} failed: ${response.status}`)
  }

  return response.json() as Promise<T>
}
