const tones = {
  low: 'bg-teal-50 text-teal-700',
  moderate: 'bg-amber-100 text-amber-600',
  high: 'bg-coral-100 text-coral-700',
  neutral: 'bg-paper-dim text-ink-soft',
} as const

export function Badge({
  tone = 'neutral',
  icon,
  children,
}: {
  tone?: keyof typeof tones
  icon?: React.ReactNode
  children: React.ReactNode
}) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-sm font-medium ${tones[tone]}`}
    >
      {icon && (
        <span aria-hidden="true" className="inline-flex">
          {icon}
        </span>
      )}
      {children}
    </span>
  )
}
