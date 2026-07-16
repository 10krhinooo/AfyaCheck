import { ExternalLink, MapPin, Phone } from 'lucide-react'
import { Card } from '../../components/Card'
import type { HealthCenter } from './types'

// The default, always-rendered view — accessible, keyboard-operable, and independent of
// whether the map above successfully loaded. Screen reader users and anyone on a blocked/
// failed Maps JS load rely on this, per the WCAG requirement for this route.
export function CenterList({ centers }: { centers: HealthCenter[] }) {
  return (
    <ol className="mt-6 space-y-3">
      {centers.map((center) => (
        <li key={center.id}>
          <Card interactive className="flex flex-col gap-4 p-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p className="font-medium text-ink">{center.name}</p>
              <p className="mt-1 flex items-start gap-1.5 text-sm text-ink-soft">
                <MapPin aria-hidden="true" size={14} className="mt-0.5 flex-shrink-0 text-teal-500" />
                {center.address}
              </p>
              {center.phone && (
                <a
                  href={`tel:${center.phone}`}
                  className="mt-1 flex items-center gap-1.5 text-sm text-teal-600 hover:text-teal-700"
                >
                  <Phone aria-hidden="true" size={14} className="flex-shrink-0" />
                  {center.phone}
                </a>
              )}
            </div>
            <div className="flex flex-shrink-0 items-center gap-4 sm:flex-col sm:items-end sm:gap-1">
              {center.distanceKm !== undefined && (
                <span className="whitespace-nowrap text-sm text-teal-600">{center.distanceKm} km</span>
              )}
              <a
                href={`https://www.google.com/maps/dir/?api=1&destination=${center.lat},${center.lng}`}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-1 whitespace-nowrap text-sm font-medium text-teal-600 hover:text-teal-700"
              >
                Directions
                <ExternalLink aria-hidden="true" size={14} />
              </a>
            </div>
          </Card>
        </li>
      ))}
    </ol>
  )
}
