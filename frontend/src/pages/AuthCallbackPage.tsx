import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

/**
 * Landing spot for the browser after a Google/GitHub OAuth2 round trip.
 * The backend has already set the refresh token cookie by the time we get
 * here; all this page does is mint an access token from it and route to
 * the right place.
 */
export function AuthCallbackPage() {
  const { refresh, user, isAdmin } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    refresh().then(() => {
      navigate('/dashboard', { replace: true })
    })
    // Only run once, on mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (user && isAdmin) {
      navigate('/admin/dashboard', { replace: true })
    }
  }, [user, isAdmin, navigate])

  return <div className="page-loading text-center py-5">Signing you in...</div>
}
