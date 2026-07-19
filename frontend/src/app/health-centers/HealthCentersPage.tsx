import { Lock, MapPin } from 'lucide-react'
import { useState } from 'react'
import { Button } from '../../components/Button'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { CenterList } from './CenterList'
import { KENYA_COUNTIES } from './counties'
import { MapView } from './MapView'
import { useGoogleMaps } from './useGoogleMaps'
import { useNearbyHealthCenters } from './useNearbyHealthCenters'

export default function HealthCentersPage() {
  const { status: mapsStatus, retry: retryMaps } = useGoogleMaps()
  const { status, centers, origin, error, locationDenied, searchFrom, retry: retryCenters } =
    useNearbyHealthCenters(mapsStatus === 'ready')
  const [county, setCounty] = useState('')
  const [stiOnly, setStiOnly] = useState(false)

  // Live Places results have unknown STI-testing status — the filter only excludes centers
  // explicitly curated as not offering it.
  const visibleCenters = stiOnly
    ? centers.filter((center) => center.stiTestingAvailable !== false)
    : centers

  function searchFromCounty() {
    const selected = KENYA_COUNTIES.find((c) => c.name === county)
    if (selected) void searchFrom({ lat: selected.lat, lng: selected.lng })
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-8 sm:py-12 lg:py-16">
      <h1 className="font-display text-2xl text-ink sm:text-3xl">Health centers near you</h1>
      <p className="mt-2 flex items-start gap-2 text-ink-soft">
        <Lock aria-hidden="true" size={16} className="mt-1 flex-shrink-0 text-teal-500" />
        <span>
          We use your device location to find nearby testing and care options. Nothing is shared
          beyond this search.
        </span>
      </p>

      {mapsStatus === 'ready' && status === 'done' && origin && visibleCenters.length > 0 && (
        <div className="mt-6">
          <MapView origin={origin} centers={visibleCenters} />
        </div>
      )}

      {(status === 'locating' || status === 'searching') && (
        <div className="mt-6" aria-live="polite">
          <span className="sr-only">
            {status === 'locating' ? 'Finding your location…' : 'Searching for nearby centers…'}
          </span>
          <Skeleton className="h-56 w-full sm:h-72 lg:h-96" />
        </div>
      )}

      {mapsStatus === 'error' && (
        <div className="mt-6">
          <StatusMessage
            tone="error"
            action={
              <Button variant="secondary" onClick={retryMaps}>
                Try again
              </Button>
            }
          >
            We couldn’t load the map, so we can’t search for nearby health centers right now. Check your
            connection and try again.
          </StatusMessage>
        </div>
      )}

      {mapsStatus === 'ready' && status === 'error' && (
        <div className="mt-6">
          <StatusMessage
            tone="error"
            action={
              locationDenied ? undefined : (
                <Button variant="secondary" onClick={retryCenters}>
                  Try again
                </Button>
              )
            }
          >
            {error}
          </StatusMessage>

          {locationDenied && (
            <form
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
              onSubmit={(e) => {
                e.preventDefault()
                searchFromCounty()
              }}
            >
              <label className="flex-1 text-sm text-ink">
                <span className="mb-1 flex items-center gap-1.5 font-medium">
                  <MapPin aria-hidden="true" size={14} className="text-teal-500" />
                  Search from a county instead
                </span>
                <select
                  value={county}
                  onChange={(e) => setCounty(e.target.value)}
                  className="w-full rounded-xl border border-teal-100 bg-white px-4 py-2.5 text-ink focus:border-teal-400 focus:outline-none focus:ring-2 focus:ring-teal-100"
                  required
                >
                  <option value="" disabled>
                    Choose your county…
                  </option>
                  {KENYA_COUNTIES.map((c) => (
                    <option key={c.name} value={c.name}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </label>
              <Button type="submit" variant="primary" disabled={county === ''}>
                Search
              </Button>
            </form>
          )}
        </div>
      )}

      {status === 'done' && centers.length > 0 && (
        <label className="mt-6 flex cursor-pointer items-center gap-2 text-sm text-ink">
          <input
            type="checkbox"
            checked={stiOnly}
            onChange={(e) => setStiOnly(e.target.checked)}
            className="h-4 w-4 accent-teal-600"
          />
          Only show centers with STI testing
        </label>
      )}

      {status === 'done' && visibleCenters.length === 0 && (
        <p className="mt-6 text-ink-soft">
          {centers.length > 0
            ? 'No centers matching this filter were found nearby. Clear the filter to see all centers.'
            : 'No health centers were found within 10km of this location. Try again later, or search from a different location.'}
        </p>
      )}

      {visibleCenters.length > 0 && <CenterList centers={visibleCenters} />}
    </main>
  )
}
