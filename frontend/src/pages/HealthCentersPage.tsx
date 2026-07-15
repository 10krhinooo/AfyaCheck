import { useEffect, useRef, useState } from 'react'

// Minimal shape of what we read off google.maps.places results; the full
// type is huge and the Maps JS SDK's own types aren't loaded here.
interface PlaceResult {
  name?: string
  vicinity?: string
  rating?: number
  user_ratings_total?: number
  types?: string[]
  geometry?: { location: { lat: () => number; lng: () => number } }
  opening_hours?: { open_now?: boolean }
}

declare global {
  interface Window {
    google: typeof google
    initAfyaMap?: () => void
  }
}

const DEFAULT_LOCATION = { lat: -1.3123, lng: 36.8118 }

function calculateDistance(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLon = ((lon2 - lon1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * Math.sin(dLon / 2) * Math.sin(dLon / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

function loadGoogleMapsScript(apiKey: string): Promise<void> {
  return new Promise((resolve, reject) => {
    if (window.google?.maps) {
      resolve()
      return
    }
    window.initAfyaMap = () => resolve()
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&callback=initAfyaMap&libraries=places&v=weekly`
    script.async = true
    script.defer = true
    script.onerror = () => reject(new Error('Failed to load Google Maps'))
    document.head.appendChild(script)
  })
}

interface CenterWithDistance {
  place: PlaceResult
  distanceKm: number
}

export function HealthCentersPage() {
  const mapDivRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<google.maps.Map | null>(null)
  const placesServiceRef = useRef<google.maps.places.PlacesService | null>(null)
  const markersRef = useRef<google.maps.Marker[]>([])

  const [status, setStatus] = useState('Loading Google Maps...')
  const [centers, setCenters] = useState<CenterWithDistance[]>([])
  const [radius, setRadius] = useState(5000)
  const [facilityType, setFacilityType] = useState('hospital')

  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY as string | undefined

  useEffect(() => {
    if (!apiKey) {
      setStatus('Google Maps API key is not configured.')
      return
    }

    loadGoogleMapsScript(apiKey)
      .then(() => {
        if (!mapDivRef.current) return

        mapRef.current = new window.google.maps.Map(mapDivRef.current, {
          center: DEFAULT_LOCATION,
          zoom: 12,
        })
        placesServiceRef.current = new window.google.maps.places.PlacesService(mapRef.current)

        setStatus('Detecting your location...')
        detectLocation()
      })
      .catch(() => setStatus('Failed to load Google Maps. Check your connection.'))
    // Only run once on mount.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiKey])

  function detectLocation() {
    if (!navigator.geolocation) {
      useLocation(DEFAULT_LOCATION)
      return
    }

    navigator.geolocation.getCurrentPosition(
      (position) => useLocation({ lat: position.coords.latitude, lng: position.coords.longitude }),
      () => useLocation(DEFAULT_LOCATION),
      { enableHighAccuracy: true, timeout: 8000, maximumAge: 300000 },
    )
  }

  function useLocation(location: { lat: number; lng: number }) {
    if (!mapRef.current) return

    new window.google.maps.Marker({
      position: location,
      map: mapRef.current,
      title: 'Your Location',
      icon: { path: window.google.maps.SymbolPath.CIRCLE, scale: 10, fillColor: '#198754', fillOpacity: 0.9, strokeColor: '#fff', strokeWeight: 2 },
    })
    mapRef.current.setCenter(location)
    mapRef.current.setZoom(14)

    searchNearby(location)
  }

  function searchNearby(location: { lat: number; lng: number }) {
    if (!placesServiceRef.current) return
    setStatus('Searching for health centers...')

    placesServiceRef.current.nearbySearch(
      { location, radius, type: facilityType },
      (results, placesStatus) => {
        if (placesStatus === window.google.maps.places.PlacesServiceStatus.OK && results) {
          const withDistance: CenterWithDistance[] = results.map((place) => ({
            place: place as PlaceResult,
            distanceKm: place.geometry?.location
              ? calculateDistance(location.lat, location.lng, place.geometry.location.lat(), place.geometry.location.lng())
              : 0,
          }))
          setCenters(withDistance)
          setStatus(`Found ${results.length} health centers nearby`)
          addMarkers(results as PlaceResult[])
        } else if (placesStatus === window.google.maps.places.PlacesServiceStatus.ZERO_RESULTS) {
          setCenters([])
          setStatus('No health centers found. Try a larger radius or different facility type.')
        } else {
          setStatus(`Error: ${placesStatus}`)
        }
      },
    )
  }

  function addMarkers(results: PlaceResult[]) {
    if (!mapRef.current) return
    markersRef.current.forEach((marker) => marker.setMap(null))
    markersRef.current = results
      .filter((place) => place.geometry?.location)
      .map((place) => {
        const marker = new window.google.maps.Marker({
          position: place.geometry!.location as unknown as google.maps.LatLng,
          map: mapRef.current!,
          title: place.name,
        })
        return marker
      })
  }

  function handleFilterChange() {
    if (!mapRef.current) return
    const center = mapRef.current.getCenter()
    if (center) {
      searchNearby({ lat: center.lat(), lng: center.lng() })
    }
  }

  return (
    <div className="container-fluid py-4">
      <div className="container">
        <h1 className="h3 mb-4 text-center">Find Health Centers Near You</h1>

        <div className="row g-3 mb-3">
          <div className="col-md-4">
            <label className="form-label">Search Radius</label>
            <select
              className="form-select"
              value={radius}
              onChange={(e) => {
                setRadius(Number(e.target.value))
                setTimeout(handleFilterChange, 0)
              }}
            >
              <option value={1000}>1 km</option>
              <option value={2000}>2 km</option>
              <option value={5000}>5 km</option>
              <option value={10000}>10 km</option>
              <option value={20000}>20 km</option>
            </select>
          </div>
          <div className="col-md-4">
            <label className="form-label">Facility Type</label>
            <select
              className="form-select"
              value={facilityType}
              onChange={(e) => {
                setFacilityType(e.target.value)
                setTimeout(handleFilterChange, 0)
              }}
            >
              <option value="hospital">Hospitals</option>
              <option value="doctor">Doctors</option>
              <option value="pharmacy">Pharmacies</option>
              <option value="health">Health Clinics</option>
              <option value="dentist">Dentists</option>
            </select>
          </div>
        </div>

        <div className="alert alert-info">{status}</div>

        <div className="row">
          <div className="col-lg-8">
            <div ref={mapDivRef} style={{ height: 500, width: '100%', borderRadius: 12 }} />
          </div>
          <div className="col-lg-4">
            <div className="list-group" style={{ maxHeight: 500, overflowY: 'auto' }}>
              {centers.map((center, index) => (
                <div className="list-group-item" key={index}>
                  <h6 className="mb-1">{center.place.name}</h6>
                  <p className="mb-1 small text-muted">{center.place.vicinity}</p>
                  <span className="badge bg-success">{center.distanceKm.toFixed(1)} km</span>
                  {center.place.rating && (
                    <span className="ms-2 text-warning">★ {center.place.rating}</span>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
