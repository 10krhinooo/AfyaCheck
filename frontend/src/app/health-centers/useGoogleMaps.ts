import { useCallback, useEffect, useState } from 'react'
import { apiFetch } from '../../lib/api-client'

declare global {
  interface Window {
    google?: typeof google
  }
}

type Status = 'loading' | 'ready' | 'error'

// Lazily loads the Google Maps JS SDK (not part of the route's base bundle budget — see
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

        const script = document.createElement('script')
        script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(apiKey)}&libraries=places`
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
