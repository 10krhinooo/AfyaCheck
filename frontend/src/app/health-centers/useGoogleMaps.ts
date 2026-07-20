import { useCallback, useEffect, useState } from 'react'
import { apiFetch } from '../../lib/api-client'

declare global {
  interface Window {
    google?: typeof google
    gm_authFailure?: () => void
  }
}

type Status = 'loading' | 'ready' | 'error'

// Lazily loads the Google Maps JS SDK (not part of the route's base bundle budget, see
// budget.json) only once an API key is available. Callers must handle 'error' gracefully:
// ad blockers, offline/3G timeouts, or a missing key are all expected on this route.
export function useGoogleMaps(): { status: Status; retry: () => void } {
  const [status, setStatus] = useState<Status>('loading')
  const [attempt, setAttempt] = useState(0)
  const retry = useCallback(() => {
    setStatus('loading')
    setAttempt((a) => a + 1)
  }, [])

  useEffect(() => {
    if (window.google?.maps) {
      setStatus('ready')
      return
    }

    let cancelled = false

    apiFetch<{ apiKey: string }>('/api/config/maps-key')
      .then(({ apiKey }) => {
        if (cancelled || !apiKey) throw new Error('Maps API key unavailable')

        // Google calls this global if the key loads but fails authorization (bad key, missing
        // HTTP-referrer allowlist entry, Maps JS API not enabled on the project), a failure
        // mode script.onload/onerror can't see, since the script itself loads successfully.
        window.gm_authFailure = () => {
          if (!cancelled) setStatus('error')
        }

        const script = document.createElement('script')
        // loading=async is Google's own recommended query param (distinct from the script
        // tag's async attribute below) — without it the JS API logs a console warning that
        // it's blocking parsing while it bootstraps.
        script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&libraries=places&loading=async`
        script.async = true
        script.onload = () => {
          if (!cancelled) setStatus('ready')
        }
        script.onerror = () => {
          if (!cancelled) setStatus('error')
        }
        document.head.appendChild(script)
      })
      .catch(() => {
        if (!cancelled) setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [attempt])

  return { status, retry }
}
