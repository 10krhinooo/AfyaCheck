import { AlertCircle, Check } from 'lucide-react'
import type { ReactNode } from 'react'

// Reuses icons already pinned/bundled elsewhere (AlertCircle from Questionnaire's error
// state, Check from QuestionForm's radio selection) instead of adding new ones, since this
// component is imported from every route including the tight client/app chunk.
const tones = {
  error: { icon: AlertCircle, className: 'text-coral-700' },
  success: { icon: Check, className: 'text-teal-700' },
} as const

export function StatusMessage({
  tone,
  children,
  action,
}: {
  tone: keyof typeof tones
  children: ReactNode
  action?: ReactNode
}) {
  const { icon: Icon, className } = tones[tone]
  return (
    <div role={tone === 'error' ? 'alert' : 'status'} className={`flex items-start gap-2 ${className}`}>
      <Icon aria-hidden="true" size={18} className="mt-0.5 flex-shrink-0" />
      <div>
        <p>{children}</p>
        {action && <div className="mt-3">{action}</div>}
      </div>
    </div>
  )
}
