import type { HTMLAttributes } from 'react'

// Decorative only, pair with aria-busy plus an aria-live status text on the containing
// landmark, same pattern already used at QuestionnairePage's loading state. Callers
// compose shapes locally (a pill, stacked lines, a tall block) via className.
export function Skeleton({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      aria-hidden="true"
      className={`rounded-[var(--radius-card)] bg-paper-dim motion-safe:animate-pulse ${className}`}
      {...props}
    />
  )
}
