import { useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Card } from '../../components/Card'
import { LinkButton } from '../../components/Button'
import { apiFetch } from '../../lib/api-client'

interface DashboardResponse {
  username: string
  isAdmin: boolean
}

export default function DashboardPage() {
  const [data, setData] = useState<DashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiFetch<DashboardResponse>('/api/dashboard')
      .then(setData)
      .catch((err: Error) => setError(err.message))
  }, [])

  if (error) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center">
        <p className="text-coral-700">{error}</p>
      </main>
    )
  }
  if (!data) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-16 text-center text-ink-soft" aria-busy="true">
        Loading…
      </main>
    )
  }
  if (data.isAdmin) return <Navigate to="/app/admin" replace />

  return (
    <main className="mx-auto max-w-2xl px-6 py-12">
      <h1 className="text-2xl text-ink">Welcome back, {data.username}</h1>
      <p className="mt-2 text-ink-soft">Continue your assessment or explore your options.</p>

      <div className="mt-8 grid gap-4 sm:grid-cols-2">
        <Card className="p-6">
          <h2 className="text-lg text-ink">Start a new assessment</h2>
          <p className="mt-2 text-sm text-ink-soft">Answer a few private questions to check your risk.</p>
          <LinkButton href="/app/questionnaire" variant="primary" className="mt-4">
            Start assessment
          </LinkButton>
        </Card>
        <Card className="p-6">
          <h2 className="text-lg text-ink">Find care nearby</h2>
          <p className="mt-2 text-sm text-ink-soft">Locate a health center near you for testing or follow-up.</p>
          <LinkButton href="/app/health-centers" variant="secondary" className="mt-4">
            Find health centers
          </LinkButton>
        </Card>
      </div>
    </main>
  )
}
