import type { InputHTMLAttributes } from 'react'

// Shared styled label+input. `visuallyHiddenLabel` covers the questionnaire's large,
// unlabeled-looking inputs (label is sr-only, question heading already states the
// prompt); the visible-label sizing covers admin's denser form inputs.
export function TextField({
  label,
  visuallyHiddenLabel = false,
  className = '',
  ...props
}: InputHTMLAttributes<HTMLInputElement> & { label: string; visuallyHiddenLabel?: boolean }) {
  return (
    <label className="block">
      <span className={visuallyHiddenLabel ? 'sr-only' : 'text-sm text-ink-soft'}>{label}</span>
      <input
        className={`${visuallyHiddenLabel ? '' : 'mt-1'} w-full rounded-xl border border-teal-100 bg-white text-ink transition-shadow duration-200 focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-300/40 ${
          visuallyHiddenLabel ? 'px-4 py-3 text-lg' : 'px-3 py-2 text-sm'
        } ${className}`}
        {...props}
      />
    </label>
  )
}
