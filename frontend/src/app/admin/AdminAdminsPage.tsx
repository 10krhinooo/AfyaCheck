import { useState } from 'react'
import { AdminNav } from './AdminNav'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { StatusMessage } from '../../components/StatusMessage'
import { TextField } from '../../components/TextField'
import { apiPost } from '../../lib/api-client'

const GENERIC_ERROR = 'Something went wrong. Check your connection and try again.'

export default function AdminAdminsPage() {
  const [email, setEmail] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    setSubmitting(true)
    try {
      const result = await apiPost<{ message: string }>('/api/admin/users/promote', { email })
      setSuccess(result.message)
      setEmail('')
    } catch (err) {
      setError(err instanceof Error ? err.message : GENERIC_ERROR)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="w-full px-6 py-10">
      <h1 className="font-display text-2xl text-ink">Admins</h1>
      <AdminNav />

      <Card className="max-w-md p-6">
        <p className="text-sm text-ink-soft">
          Give another registered user admin access. They need to have signed in at least once
          before you can promote them.
        </p>

        <form className="mt-4 space-y-4" onSubmit={handleSubmit}>
          <TextField
            label="Email address"
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="name@example.com"
          />
          <Button type="submit" loading={submitting} disabled={submitting}>
            Make admin
          </Button>
        </form>

        {error && (
          <div className="mt-4">
            <StatusMessage tone="error">{error}</StatusMessage>
          </div>
        )}
        {success && (
          <div className="mt-4">
            <StatusMessage tone="success">{success}</StatusMessage>
          </div>
        )}
      </Card>
    </main>
  )
}
