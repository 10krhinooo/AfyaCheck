import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LinkButton } from '../../components/Button'
import { StatusMessage } from '../../components/StatusMessage'
import { getUserManager } from './keycloak'

// Keycloak redirects here after login (see redirect_uri in keycloak.ts and the client's
// registered redirectUris in keycloak/realm-export.json).
export default function CallbackPage() {
  const navigate = useNavigate()
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    getUserManager()
      .signinRedirectCallback()
      .then((user) => {
        const returnTo = (user.state as { returnTo?: string } | undefined)?.returnTo ?? '/app/dashboard'
        navigate(returnTo, { replace: true })
      })
      .catch((err: Error) => setError(err.message))
  }, [navigate])

  if (error) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center">
        <div className="inline-flex text-left">
          <StatusMessage tone="error">
            We couldn’t sign you in ({error}). Return home and try signing in again.
          </StatusMessage>
        </div>
        <div>
          <LinkButton href="/" variant="secondary" className="mt-6">
            Return home
          </LinkButton>
        </div>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-xl px-6 py-16 text-center text-ink-soft" aria-busy="true">
      Signing you in…
    </main>
  )
}
