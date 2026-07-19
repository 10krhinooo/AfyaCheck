import { useState, type FormEvent } from 'react'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { StatusMessage } from '../../components/StatusMessage'
import { apiPost } from '../../lib/api-client'
import { useTranslation } from '../../lib/i18n'

// Opt-in retest reminder. The backend stores the email + send date only (no link to this
// assessment) and deletes it after the one reminder is sent — the hint copy says so.
export function RemindMeForm() {
  const { t } = useTranslation()
  const [email, setEmail] = useState('')
  const [sending, setSending] = useState(false)
  const [status, setStatus] = useState<'idle' | 'scheduled' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (email.trim() === '') return

    setSending(true)
    setStatus('idle')
    setErrorMessage(null)
    try {
      await apiPost('/api/reminders', { email: email.trim() })
      setStatus('scheduled')
    } catch (err) {
      setStatus('error')
      setErrorMessage(err instanceof Error ? err.message : t('results.remindError'))
    } finally {
      setSending(false)
    }
  }

  if (status === 'scheduled') {
    return <StatusMessage tone="success">{t('results.remindScheduled')}</StatusMessage>
  }

  return (
    <form onSubmit={handleSubmit}>
      <p className="text-sm text-ink-soft">{t('results.remindHint')}</p>
      <div className="mt-3 flex flex-col gap-3 sm:flex-row sm:items-end">
        <div className="flex-1">
          <TextField
            label={t('results.remindLabel')}
            type="email"
            placeholder="you@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />
        </div>
        <Button type="submit" variant="secondary" loading={sending} disabled={email.trim() === ''}>
          {t('results.remindCta')}
        </Button>
      </div>
      {status === 'error' && errorMessage && (
        <div className="mt-3">
          <StatusMessage tone="error">{errorMessage}</StatusMessage>
        </div>
      )}
    </form>
  )
}
