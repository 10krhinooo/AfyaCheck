import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
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
        <p className="text-coral-700">Sign-in failed: {error}</p>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-xl px-6 py-16 text-center text-ink-soft" aria-busy="true">
      Signing you in…
    </main>
  )
}
