import { ClipboardList, MapPin } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { LinkButton } from '../../components/Button'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { apiFetch } from '../../lib/api-client'

interface DashboardResponse {
  username: string
  isAdmin: boolean
}

const LOAD_ERROR = 'We couldn’t load your dashboard. Check your connection and try again.'

export default function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    setError(null)
    apiFetch<DashboardResponse>('/api/dashboard')
      .then(setData)
      .catch(() => setError(LOAD_ERROR))
  }, [attempt])

  if (error) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center">
        <div className="inline-flex text-left">
          <StatusMessage tone="error">{error}</StatusMessage>
        </div>
        <div>
          <Button variant="secondary" className="mt-6" onClick={() => setAttempt((a) => a + 1)}>
            Try again
          </Button>
        </div>
      </main>
    )
  }
  if (!data) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-12" aria-busy="true">
        <span className="sr-only">Loading…</span>
        <Skeleton className="h-8 w-64" />
        <Skeleton className="mt-2 h-4 w-80" />
        <div className="mt-8 grid gap-4 sm:grid-cols-2">
          <Skeleton className="h-32" />
          <Skeleton className="h-32" />
        </div>
      </main>
    )
  }
  if (data.isAdmin) return <Navigate to="/app/admin" replace />

  return (
    <main className="mx-auto max-w-2xl px-6 py-12">
      <h1 className="font-display text-3xl text-ink">Welcome back, {data.username}</h1>
      <p className="mt-2 text-ink-soft">Continue your assessment or explore your options.</p>

      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        <Card interactive className="p-6">
          <h2 className="flex items-center gap-2 text-lg text-ink">
            <ClipboardList aria-hidden="true" size={20} className="text-teal-600" />
            Start a new assessment
          </h2>
          <p className="mt-2 text-sm text-ink-soft">Answer a few private questions to check your risk.</p>
          <LinkButton href="/app/questionnaire" variant="primary" className="mt-4">
            Start assessment
          </LinkButton>
        </Card>
        <Card interactive className="p-6">
          <h2 className="flex items-center gap-2 text-lg text-ink">
            <MapPin aria-hidden="true" size={20} className="text-teal-600" />
            Find care nearby
          </h2>
          <p className="mt-2 text-sm text-ink-soft">Locate a health center near you for testing or follow-up.</p>
          <LinkButton href="/app/health-centers" variant="secondary" className="mt-4">
            Find health centers
          </LinkButton>
        </Card>
      </div>
    </main>
  )
}
