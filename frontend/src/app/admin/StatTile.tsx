import { Card } from '../../components/Card'

export function StatTile({
  label,
  value,
  icon,
}: {
  label: string
  value: number | string
  icon?: React.ReactNode
  // Not populated yet: AdminDashboardResponse has no trend/delta field today.
  // Reserved so StatTile is ready once the backend exposes one.
  trend?: { value: number; direction: 'up' | 'down' }
}) {
  return (
    <Card interactive className="p-5">
      <div className="flex items-center gap-2">
        {icon && (
          <span aria-hidden="true" className="text-teal-500">
            {icon}
          </span>
        )}
        <p className="text-sm text-ink-soft">{label}</p>
      </div>
      <p className="mt-1 font-display text-3xl text-ink">{value}</p>
    </Card>
  )
}
