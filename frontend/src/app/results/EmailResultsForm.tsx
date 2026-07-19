import { useState, type FormEvent } from 'react'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { StatusMessage } from '../../components/StatusMessage'
import { apiPost } from '../../lib/api-client'
import { useTranslation } from '../../lib/i18n'

export function EmailResultsForm({ sessionId }: { sessionId: string }) {
  const { t } = useTranslation()
  const [email, setEmail] = useState('')
  const [sending, setSending] = useState(false)
  const [status, setStatus] = useState<'idle' | 'sent' | 'error'>('idle')
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (email.trim() === '') return

    setSending(true)
    setStatus('idle')
    setErrorMessage(null)
    try {
      await apiPost('/api/results/notify', { sessionId, email })
      setStatus('sent')
    } catch (err) {
      setStatus('error')
      setErrorMessage(err instanceof Error ? err.message : t('results.sendError'))
    } finally {
      setSending(false)
    }
  }

  if (status === 'sent') {
    return (
      <StatusMessage tone="success">{t('results.sent')}</StatusMessage>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
      <div className="flex-1">
        <TextField
          label={t('results.emailLabel')}
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>
      <Button type="submit" variant="secondary" loading={sending} disabled={email.trim() === ''}>
        {t('results.send')}
      </Button>
      {status === 'error' && errorMessage && (
        <div className="sm:basis-full">
          <StatusMessage tone="error">{errorMessage}</StatusMessage>
        </div>
      )}
    </form>
  )
}
