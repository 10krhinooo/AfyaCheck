import { Lock } from 'lucide-react'
import { Button } from '../../components/Button'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { CenterList } from './CenterList'
import { MapView } from './MapView'
import { useGoogleMaps } from './useGoogleMaps'
import { useNearbyHealthCenters } from './useNearbyHealthCenters'

export default function HealthCentersPage() {
  const { status: mapsStatus, retry: retryMaps } = useGoogleMaps()
  const { status, centers, origin, error, retry: retryCenters } = useNearbyHealthCenters(mapsStatus === 'ready')

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

      {mapsStatus === 'ready' && status === 'done' && origin && centers.length > 0 && (
        <div className="mt-6">
          <MapView origin={origin} centers={centers} />
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
              <Button variant="secondary" onClick={retryCenters}>
                Try again
              </Button>
            }
          >
            {error}
          </StatusMessage>
        </div>
      )}

      {status === 'done' && centers.length === 0 && (
        <p className="mt-6 text-ink-soft">
          No health centers were found within 10km of your location. Try again later, or search from a
          different location.
        </p>
      )}

      {centers.length > 0 && <CenterList centers={centers} />}
    </main>
  )
}
