import { useEffect } from 'react'
import type { ReactNode } from 'react'
import { LinkButton } from '../../components/Button'
import { StatusMessage } from '../../components/StatusMessage'
import { useAuth } from './AuthContext'
import { login } from './keycloak'

// Wraps a route that requires a signed-in user (dashboard, admin). Unauthenticated visitors
// are sent straight into Keycloak's hosted login rather than shown a 401/broken page — the
// public flows (questionnaire, results, health-centers, landing) never use this.
export function RequireAuth({ children, adminOnly = false }: { children: ReactNode; adminOnly?: boolean }) {
  const { isLoading, isAuthenticated, isAdmin } = useAuth()

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      login(window.location.pathname)
    }
  }, [isLoading, isAuthenticated])

  if (isLoading || !isAuthenticated) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-16 text-center text-ink-soft" aria-busy="true">
        Redirecting to sign in…
      </main>
    )
  }

  if (adminOnly && !isAdmin) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-16 text-center">
        <div className="inline-flex text-left">
          <StatusMessage tone="error">
            You don’t have access to this page. It’s restricted to administrators.
          </StatusMessage>
        </div>
        <div>
          <LinkButton href="/app/dashboard" variant="secondary" className="mt-6">
            Back to dashboard
          </LinkButton>
        </div>
      </main>
    )
  }

  return <>{children}</>
}
