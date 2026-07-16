import { useEffect, useRef } from 'react'
import type { HealthCenter } from './types'

// Brand-colored markers, replacing Google's default red pin (off-brand for a calm,
// non-clinical identity). Data URIs can't reference CSS custom properties, so the hex
// values here are hardcoded copies of --color-teal-500/600 from index.css.
const ORIGIN_ICON =
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"><circle cx="8" cy="8" r="6" fill="#2f7d6d" stroke="white" stroke-width="2"/></svg>',
  )
const CENTER_ICON =
  'data:image/svg+xml,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="28" height="36" viewBox="0 0 28 36">' +
      '<path d="M14 0C6.3 0 0 6.3 0 14c0 10.5 14 22 14 22s14-11.5 14-22C28 6.3 21.7 0 14 0z" fill="#256456"/>' +
      '<circle cx="14" cy="14" r="5" fill="white"/></svg>',
  )

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

    new google.maps.Marker({
      position: origin,
      map,
      title: 'Your location',
      icon: {
        url: ORIGIN_ICON,
        scaledSize: new google.maps.Size(16, 16),
        anchor: new google.maps.Point(8, 8),
      },
    })
    centers.forEach((center) => {
      new google.maps.Marker({
        position: { lat: center.lat, lng: center.lng },
        map,
        title: center.name,
        icon: {
          url: CENTER_ICON,
          scaledSize: new google.maps.Size(28, 36),
          anchor: new google.maps.Point(14, 36),
        },
      })
    })
  }, [origin, centers])

  return (
    <div
      ref={containerRef}
      role="img"
      aria-label={`Map showing ${centers.length} health centers near you`}
      className="h-56 w-full rounded-[var(--radius-card)] bg-teal-50 sm:h-72 lg:h-96"
    />
  )
}
