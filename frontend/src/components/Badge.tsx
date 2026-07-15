const tones = {
  low: 'bg-teal-50 text-teal-700',
  moderate: 'bg-amber-100 text-amber-600',
  high: 'bg-coral-100 text-coral-700',
  neutral: 'bg-paper-dim text-ink-soft',
} as const

export function Badge({ tone = 'neutral', children }: { tone?: keyof typeof tones; children: React.ReactNode }) {
  return (
    <span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${tones[tone]}`}>
      {children}
    </span>
  )
}
