import { CenterList } from './CenterList'
import { MapView } from './MapView'
import { useGoogleMaps } from './useGoogleMaps'
import { useNearbyHealthCenters } from './useNearbyHealthCenters'

export default function HealthCentersPage() {
  const mapsStatus = useGoogleMaps()
  const { status, centers, origin, error } = useNearbyHealthCenters(mapsStatus === 'ready')

  return (
    <main className="mx-auto max-w-2xl px-6 py-12">
      <h1 className="text-2xl text-ink">Health centers near you</h1>
      <p className="mt-2 text-ink-soft">
        We use your device location to find nearby testing and care options. Nothing is shared
        beyond this search.
      </p>

      {mapsStatus === 'ready' && status === 'done' && origin && centers.length > 0 && (
        <div className="mt-6">
          <MapView origin={origin} centers={centers} />
        </div>
      )}

      {(status === 'locating' || status === 'searching') && (
        <p className="mt-6 text-ink-soft" aria-live="polite">
          {status === 'locating' ? 'Finding your location…' : 'Searching for nearby centers…'}
        </p>
      )}

      {(status === 'error' || mapsStatus === 'error') && (
        <p className="mt-6 text-coral-700" role="alert">
          {error ?? 'The map couldn’t load. You can still browse the list below once results are available.'}
        </p>
      )}

      {status === 'done' && centers.length === 0 && (
        <p className="mt-6 text-ink-soft">No health centers were found within 10km of your location.</p>
      )}

      {centers.length > 0 && <CenterList centers={centers} />}
    </main>
  )
}
