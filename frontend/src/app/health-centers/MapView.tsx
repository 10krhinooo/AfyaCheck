import { useEffect, useRef } from 'react'
import type { HealthCenter } from './types'

export function MapView({
  origin,
  centers,
}: {
  origin: { lat: number; lng: number }
  centers: HealthCenter[]
}) {
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!containerRef.current) return
    const map = new google.maps.Map(containerRef.current, {
      center: origin,
      zoom: 12,
      disableDefaultUI: true,
      zoomControl: true,
    })

    new google.maps.Marker({ position: origin, map, title: 'Your location' })
    centers.forEach((center) => {
      new google.maps.Marker({ position: { lat: center.lat, lng: center.lng }, map, title: center.name })
    })
  }, [origin, centers])

  return (
    <div
      ref={containerRef}
      role="img"
      aria-label={`Map showing ${centers.length} health centers near you`}
      className="h-72 w-full rounded-[var(--radius-card)] bg-teal-50"
    />
  )
}
