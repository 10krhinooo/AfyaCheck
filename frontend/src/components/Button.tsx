import { type ButtonHTMLAttributes, type AnchorHTMLAttributes, forwardRef } from 'react'

const base =
  'inline-flex items-center justify-center gap-2 rounded-full font-medium ' +
  'transition-[color,background-color,transform] duration-200 ease-editorial ' +
  'motion-safe:hover:-translate-y-0.5 motion-safe:active:translate-y-0 ' +
  'disabled:opacity-40 disabled:pointer-events-none disabled:hover:translate-y-0'

const variants = {
  primary: 'bg-teal-600 text-paper hover:bg-teal-700 px-6 py-3 text-base',
  secondary: 'bg-teal-50 text-teal-700 hover:bg-teal-100 px-6 py-3 text-base',
  ghost: 'text-ink-soft hover:text-ink hover:bg-paper-dim px-4 py-2 text-sm',
} as const

type Variant = keyof typeof variants

function Spinner() {
  return (
    <svg
      aria-hidden="true"
      className="h-4 w-4 motion-safe:animate-spin"
      viewBox="0 0 24 24"
      fill="none"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z"
      />
    </svg>
  )
}

export const Button = forwardRef<
  HTMLButtonElement,
  ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; loading?: boolean }
>(({ variant = 'primary', className = '', loading = false, disabled, children, ...props }, ref) => (
  <button
    ref={ref}
    aria-busy={loading || undefined}
    disabled={disabled || loading}
    className={`${base} ${variants[variant]} ${className}`}
    {...props}
  >
    {loading && <Spinner />}
    {children}
  </button>
))
Button.displayName = 'Button'

export function LinkButton({
  variant = 'primary',
  className = '',
  ...props
}: AnchorHTMLAttributes<HTMLAnchorElement> & { variant?: Variant }) {
  return <a className={`${base} ${variants[variant]} ${className}`} {...props} />
}
