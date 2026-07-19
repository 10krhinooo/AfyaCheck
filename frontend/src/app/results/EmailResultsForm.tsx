import { useState, type FormEvent } from 'react'
import { Button } from '../../components/Button'
import { TextField } from '../../components/TextField'
import { StatusMessage } from '../../components/StatusMessage'
import { apiPost } from '../../lib/api-client'

export function EmailResultsForm({ sessionId }: { sessionId: string }) {
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
      setErrorMessage(err instanceof Error ? err.message : 'We couldn’t send that. Please try again.')
    } finally {
      setSending(false)
    }
  }

  if (status === 'sent') {
    return (
      <StatusMessage tone="success">Sent — check your inbox for a copy of your results.</StatusMessage>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-3 sm:flex-row sm:items-end">
      <div className="flex-1">
        <TextField
          label="Email me these results"
          type="email"
          placeholder="you@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
      </div>
      <Button type="submit" variant="secondary" loading={sending} disabled={email.trim() === ''}>
        Send
      </Button>
      {status === 'error' && errorMessage && (
        <div className="sm:basis-full">
          <StatusMessage tone="error">{errorMessage}</StatusMessage>
        </div>
      )}
    </form>
  )
}
