import { useCallback, useEffect, useState } from 'react'
import type { HealthCenter } from './types'

type SearchStatus = 'idle' | 'locating' | 'searching' | 'done' | 'error'

function distanceKm(a: { lat: number; lng: number }, b: { lat: number; lng: number }) {
  const R = 6371
  const dLat = ((b.lat - a.lat) * Math.PI) / 180
  const dLng = ((b.lng - a.lng) * Math.PI) / 180
  const x =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((a.lat * Math.PI) / 180) * Math.cos((b.lat * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x))
}

export function useNearbyHealthCenters(mapsReady: boolean) {
  const [status, setStatus] = useState<SearchStatus>('idle')
  const [centers, setCenters] = useState<HealthCenter[]>([])
  const [origin, setOrigin] = useState<{ lat: number; lng: number } | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  const retry = useCallback(() => {
    setError(null)
    setAttempt((a) => a + 1)
  }, [])

  useEffect(() => {
    if (!mapsReady) return

    setStatus('locating')
    navigator.geolocation.getCurrentPosition(
      (position) => {
        const here = { lat: position.coords.latitude, lng: position.coords.longitude }
        setOrigin(here)
        setStatus('searching')

        const service = new google.maps.places.PlacesService(document.createElement('div'))
        service.nearbySearch(
          { location: here, radius: 10000, type: 'hospital' },
          (results, placesStatus) => {
            if (placesStatus === google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
              setCenters([])
              setStatus('done')
              return
            }
            if (placesStatus !== google.maps.places.PlacesServiceStatus.OK || !results) {
              setError('We couldn’t search for nearby health centers right now. Check your connection and try again.')
              setStatus('error')
              return
            }

            const found: HealthCenter[] = results
              .filter((r) => r.geometry?.location)
              .map((r) => {
                const location = { lat: r.geometry!.location!.lat(), lng: r.geometry!.location!.lng() }
                return {
                  id: r.place_id ?? r.name ?? crypto.randomUUID(),
                  name: r.name ?? 'Unnamed health center',
                  address: r.vicinity ?? 'Address unavailable',
                  lat: location.lat,
                  lng: location.lng,
                  distanceKm: Math.round(distanceKm(here, location) * 10) / 10,
                }
              })
              .sort((a, b) => (a.distanceKm ?? 0) - (b.distanceKm ?? 0))

            setCenters(found)
            setStatus('done')
          },
        )
      },
      () => {
        setError(
          'Location access was denied, so we can’t show nearby centers. Enable location access for this site in your browser settings, then try again.',
        )
        setStatus('error')
      },
    )
  }, [mapsReady, attempt])

  return { status, centers, origin, error, retry }
}
