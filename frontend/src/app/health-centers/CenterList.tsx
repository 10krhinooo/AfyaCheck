import { Clock, ExternalLink, EyeOff, MapPin, Phone } from 'lucide-react'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import type { HealthCenter } from './types'

// The default, always-rendered view: accessible, keyboard-operable, and independent of
// whether the map above successfully loaded. Screen reader users and anyone on a blocked/
// failed Maps JS load rely on this, per the WCAG requirement for this route.
export function CenterList({
  centers,
  canHide,
  onHide,
}: {
  centers: HealthCenter[]
  // Only signed-in admins can hide centers; live Places results are the only ones that need
  // it since curated (admin-entered) rows already have a delete action in the admin panel.
  canHide?: boolean
  onHide?: (center: HealthCenter) => void
}) {
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
              {center.hours && (
                <p className="mt-1 flex items-start gap-1.5 text-sm text-ink-soft">
                  <Clock aria-hidden="true" size={14} className="mt-0.5 flex-shrink-0 text-teal-500" />
                  {center.hours}
                </p>
              )}
              {(center.stiTestingAvailable || (center.services?.length ?? 0) > 0) && (
                <p className="mt-2 flex flex-wrap gap-1.5">
                  {center.stiTestingAvailable && <Badge tone="low">STI testing</Badge>}
                  {center.services?.map((service) => (
                    <span
                      key={service}
                      className="rounded-full bg-teal-50 px-2.5 py-0.5 text-xs text-teal-700"
                    >
                      {service}
                    </span>
                  ))}
                </p>
              )}
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
              {canHide && center.source === 'live' && (
                <Button
                  variant="secondary"
                  className="!px-2.5 !py-1 !text-xs"
                  onClick={() => onHide?.(center)}
                >
                  <EyeOff aria-hidden="true" size={12} className="mr-1 inline" />
                  Hide this center
                </Button>
              )}
            </div>
          </Card>
        </li>
      ))}
    </ol>
  )
}
