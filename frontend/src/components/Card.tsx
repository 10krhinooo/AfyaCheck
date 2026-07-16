import type { HTMLAttributes } from 'react'

export function Card({
  className = '',
  interactive = false,
  ...props
}: HTMLAttributes<HTMLDivElement> & { interactive?: boolean }) {
  return (
    <div
      className={`rounded-[var(--radius-card)] bg-white shadow-[var(--shadow-card)] ${
        interactive
          ? 'transition-shadow duration-300 ease-editorial motion-safe:hover:-translate-y-0.5 motion-safe:hover:shadow-[var(--shadow-card-hover)] motion-safe:transition-[transform,box-shadow]'
          : ''
      } ${className}`}
      {...props}
    />
  )
}
