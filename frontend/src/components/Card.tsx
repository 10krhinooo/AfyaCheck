import type { HTMLAttributes } from 'react'

export function Card({ className = '', ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`rounded-[var(--radius-card)] bg-white shadow-[var(--shadow-card)] ${className}`}
      {...props}
    />
  )
}
