export interface HealthCenter {
  id: string
  name: string
  address: string
  phone?: string
  hours?: string
  services?: string[]
  // Only known for admin-curated centers; live Places results leave it undefined.
  stiTestingAvailable?: boolean
  distanceKm?: number
  lat: number
  lng: number
}

// Shape returned by GET /api/health-centers/nearby (admin-curated centers, see AdminController).
export interface CuratedHealthCenterDto {
  id: number
  name: string
  address: string | null
  latitude: number
  longitude: number
  phone: string | null
  hours: string | null
  services: string[] | null
  stiTestingAvailable: boolean | null
  isActive: boolean | null
}
