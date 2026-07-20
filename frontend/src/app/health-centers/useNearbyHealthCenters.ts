import { useCallback, useEffect, useState } from 'react'
import { apiFetch } from '../../lib/api-client'
import type { CuratedHealthCenterDto, HealthCenter } from './types'

type SearchStatus = 'idle' | 'locating' | 'searching' | 'done' | 'error'

// Live Places results within this distance of a curated entry are treated as the same
// place (a rough proxy for "same building"), so the curated (vetted, richer) row wins
// instead of showing both.
const DEDUPE_RADIUS_KM = 0.08

function distanceKm(a: { lat: number; lng: number }, b: { lat: number; lng: number }) {
  const R = 6371
  const dLat = ((b.lat - a.lat) * Math.PI) / 180
  const dLng = ((b.lng - a.lng) * Math.PI) / 180
  const x =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((a.lat * Math.PI) / 180) * Math.cos((b.lat * Math.PI) / 180) * Math.sin(dLng / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(x), Math.sqrt(1 - x))
}

function placesNearbySearch(here: { lat: number; lng: number }): Promise<HealthCenter[]> {
  return new Promise((resolve, reject) => {
    const service = new google.maps.places.PlacesService(document.createElement('div'))
    service.nearbySearch({ location: here, radius: 10000, type: 'hospital' }, (results, placesStatus) => {
      if (placesStatus === google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
        resolve([])
        return
      }
      if (placesStatus !== google.maps.places.PlacesServiceStatus.OK || !results) {
        reject(new Error('Live Places search failed'))
        return
      }

      const found: HealthCenter[] = results
        .filter((r) => r.geometry?.location && r.place_id)
        .map((r) => {
          const location = { lat: r.geometry!.location!.lat(), lng: r.geometry!.location!.lng() }
          return {
            id: r.place_id!,
            name: r.name ?? 'Unnamed health center',
            address: r.vicinity ?? 'Address unavailable',
            lat: location.lat,
            lng: location.lng,
            distanceKm: Math.round(distanceKm(here, location) * 10) / 10,
            source: 'live' as const,
            placeId: r.place_id!,
          }
        })
      resolve(found)
    })
  })
}

export function useNearbyHealthCenters(mapsReady: boolean) {
  const [status, setStatus] = useState<SearchStatus>('idle')
  const [centers, setCenters] = useState<HealthCenter[]>([])
  const [origin, setOrigin] = useState<{ lat: number; lng: number } | null>(null)
  const [error, setError] = useState<string | null>(null)
  // Distinguishes "geolocation denied/unavailable" from search failures: the page offers
  // the county picker for the former, a plain retry for the latter.
  const [locationDenied, setLocationDenied] = useState(false)
  const [attempt, setAttempt] = useState(0)

  const retry = useCallback(() => {
    setError(null)
    setLocationDenied(false)
    setAttempt((a) => a + 1)
  }, [])

  const hideCenter = useCallback((id: string) => {
    setCenters((prev) => prev.filter((c) => c.id !== id))
  }, [])

  const searchFrom = useCallback(async (here: { lat: number; lng: number }) => {
    setOrigin(here)
    setError(null)
    setLocationDenied(false)
    setStatus('searching')

    // Curated (admin-vetted) centers and a live Google Places search always run together
    // and get merged below; neither source blocks the other from failing independently.
    const [curatedResult, liveResult, blacklistResult] = await Promise.allSettled([
      apiFetch<CuratedHealthCenterDto[]>(`/api/health-centers/nearby?lat=${here.lat}&lng=${here.lng}`),
      placesNearbySearch(here),
      apiFetch<string[]>('/api/health-centers/blacklisted-place-ids'),
    ])

    if (curatedResult.status === 'rejected' && liveResult.status === 'rejected') {
      setError('We couldn’t search for nearby health centers right now. Check your connection and try again.')
      setStatus('error')
      return
    }

    const curated: HealthCenter[] =
      curatedResult.status === 'fulfilled'
        ? curatedResult.value.map((c) => ({
            id: `curated-${c.id}`,
            name: c.name,
            address: c.address ?? 'Address unavailable',
            phone: c.phone ?? undefined,
            hours: c.hours ?? undefined,
            services: c.services ?? undefined,
            stiTestingAvailable: c.stiTestingAvailable ?? undefined,
            lat: c.latitude,
            lng: c.longitude,
            distanceKm: Math.round(distanceKm(here, { lat: c.latitude, lng: c.longitude }) * 10) / 10,
            source: 'curated' as const,
          }))
        : []

    const blacklistedPlaceIds = new Set(blacklistResult.status === 'fulfilled' ? blacklistResult.value : [])

    const live: HealthCenter[] =
      liveResult.status === 'fulfilled'
        ? liveResult.value.filter((livePlace) => {
            if (livePlace.placeId && blacklistedPlaceIds.has(livePlace.placeId)) return false
            return !curated.some(
              (c) =>
                c.name.trim().toLowerCase() === livePlace.name.trim().toLowerCase() ||
                distanceKm({ lat: c.lat, lng: c.lng }, { lat: livePlace.lat, lng: livePlace.lng }) <=
                  DEDUPE_RADIUS_KM,
            )
          })
        : []

    const merged = [...curated, ...live].sort((a, b) => (a.distanceKm ?? 0) - (b.distanceKm ?? 0))
    setCenters(merged)
    setStatus('done')
  }, [])

  useEffect(() => {
    if (!mapsReady) return

    setStatus('locating')
    navigator.geolocation.getCurrentPosition(
      (position) => {
        void searchFrom({ lat: position.coords.latitude, lng: position.coords.longitude })
      },
      () => {
        setError(
          'Location access was denied, so we can’t detect where you are. Pick your county below to search from there instead, or enable location access and try again.',
        )
        setLocationDenied(true)
        setStatus('error')
      },
    )
  }, [mapsReady, attempt, searchFrom])

  return { status, centers, origin, error, locationDenied, searchFrom, retry, hideCenter }
}
