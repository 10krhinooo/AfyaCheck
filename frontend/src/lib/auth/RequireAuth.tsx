import { useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, LinkButton } from '../../components/Button'
import { StatusMessage } from '../../components/StatusMessage'
import { useAuth } from './AuthContext'
import { login } from './keycloak'

// Wraps a route that requires a signed-in user (dashboard, admin). Unauthenticated visitors
// are sent straight into Keycloak's hosted login rather than shown a 401/broken page, the
// public flows (questionnaire, results, health-centers, landing) never use this.
export function RequireAuth({ children, adminOnly = false }: { children: ReactNode; adminOnly?: boolean }) {
  const { isLoading, isAuthenticated, isAdmin } = useAuth()
  const [redirectError, setRedirectError] = useState<string | null>(null)
  const navigate = useNavigate()

  function redirectToLogin() {
    setRedirectError(null)
    login(window.location.pathname).catch((err) => {
      setRedirectError(err instanceof Error ? err.message : 'Sign-in failed. Please try again.')
    })
  }

  useEffect(() => {
    if (!isLoading && !isAuthenticated) {
      redirectToLogin()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps -- redirectToLogin is redefined each render, not a dependency
  }, [isLoading, isAuthenticated])

  if (redirectError) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-16 text-center">
        <div className="inline-flex text-left">
          <StatusMessage
            tone="error"
            action={
              <div className="flex gap-3">
                <Button onClick={redirectToLogin}>Try again</Button>
                <Button variant="secondary" onClick={() => navigate(-1)}>
                  Go back
                </Button>
              </div>
            }
          >
            {redirectError}
          </StatusMessage>
        </div>
      </main>
    )
  }

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
        <div className="mt-6 flex justify-center gap-3">
          <Button variant="secondary" onClick={() => navigate(-1)}>
            Go back
          </Button>
          <LinkButton href="/app/dashboard" variant="secondary">
            Back to dashboard
          </LinkButton>
        </div>
      </main>
    )
  }

  return <>{children}</>
}
