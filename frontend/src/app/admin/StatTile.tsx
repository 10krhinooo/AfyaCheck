import { Card } from '../../components/Card'

export function StatTile({ label, value }: { label: string; value: number | string }) {
  return (
    <Card className="p-5">
      <p className="text-sm text-ink-soft">{label}</p>
      <p className="mt-1 font-display text-3xl text-ink">{value}</p>
    </Card>
  )
}
