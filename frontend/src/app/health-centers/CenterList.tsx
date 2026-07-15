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
          <Card className="flex items-center justify-between gap-4 p-4">
            <div>
              <p className="font-medium text-ink">{center.name}</p>
              <p className="text-sm text-ink-soft">{center.address}</p>
            </div>
            {center.distanceKm !== undefined && (
              <span className="whitespace-nowrap text-sm text-teal-600">{center.distanceKm} km</span>
            )}
          </Card>
        </li>
      ))}
    </ol>
  )
}
