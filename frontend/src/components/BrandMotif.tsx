import type { LucideIcon } from 'lucide-react'
import { ClipboardList, MapPin, ShieldCheck } from 'lucide-react'

const defaultIcons: LucideIcon[] = [ShieldCheck, MapPin, ClipboardList]

const sizes = {
  sm: 'h-40 w-40',
  md: 'h-56 w-56',
  lg: 'h-72 w-72 sm:h-80 sm:w-80',
} as const

// Reusable decorative motif: soft teal gradient blobs with icon glyphs, standing in for
// photography we deliberately don't use (privacy-first brand, no third-party image sourcing —
// see index.css's self-hosted-fonts stance). Reused across Landing's hero, Dashboard, and the
// About/FAQ pages instead of one-off art per page.
export function BrandMotif({
  size = 'md',
  icons = defaultIcons,
  className = '',
}: {
  size?: keyof typeof sizes
  icons?: LucideIcon[]
  className?: string
}) {
  return (
    <div className={`relative mx-auto ${sizes[size]} ${className}`} aria-hidden="true">
      <div className="absolute inset-0 rounded-full bg-gradient-to-br from-teal-100 via-teal-50 to-paper-dim blur-2xl" />
      <div className="absolute left-[10%] top-[12%] h-2/3 w-2/3 rounded-[45%] bg-teal-200/70 mix-blend-multiply blur-md" />
      <div className="absolute bottom-[8%] right-[12%] h-1/2 w-1/2 rounded-[40%] bg-teal-300/60 mix-blend-multiply blur-md" />

      <div className="absolute inset-0 flex items-center justify-center">
        <div className="flex -space-x-3">
          {icons.map((Icon, i) => (
            <span
              key={i}
              className="flex h-14 w-14 items-center justify-center rounded-full bg-white text-teal-600 shadow-[var(--shadow-card)]"
              style={{ transform: `translateY(${i % 2 === 0 ? '-0.5rem' : '0.5rem'})` }}
            >
              <Icon size={22} />
            </span>
          ))}
        </div>
      </div>
    </div>
  )
}
