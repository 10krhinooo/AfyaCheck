import { type ButtonHTMLAttributes, type AnchorHTMLAttributes, forwardRef } from 'react'

const base =
  'inline-flex items-center justify-center gap-2 rounded-full font-medium transition-colors ' +
  'disabled:opacity-40 disabled:pointer-events-none'

const variants = {
  primary: 'bg-teal-600 text-paper hover:bg-teal-700 px-6 py-3 text-base',
  secondary: 'bg-teal-50 text-teal-700 hover:bg-teal-100 px-6 py-3 text-base',
  ghost: 'text-ink-soft hover:text-ink hover:bg-paper-dim px-4 py-2 text-sm',
} as const

type Variant = keyof typeof variants

export const Button = forwardRef<HTMLButtonElement, ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }>(
  ({ variant = 'primary', className = '', ...props }, ref) => (
    <button ref={ref} className={`${base} ${variants[variant]} ${className}`} {...props} />
  ),
)
Button.displayName = 'Button'

export function LinkButton({
  variant = 'primary',
  className = '',
  ...props
}: AnchorHTMLAttributes<HTMLAnchorElement> & { variant?: Variant }) {
  return <a className={`${base} ${variants[variant]} ${className}`} {...props} />
}
