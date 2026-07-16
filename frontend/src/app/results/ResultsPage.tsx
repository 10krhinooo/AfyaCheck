import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { Badge } from '../../components/Badge'
import { Button, LinkButton } from '../../components/Button'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { apiFetch } from '../../lib/api-client'
import type { LatestResultResponse, RiskAssessmentDto } from '../../lib/results-types'
import { readStoredResult } from './resultStorage'

const riskTone = { Low: 'low', Medium: 'moderate', High: 'high' } as const
const NO_SESSION_ERROR = 'We couldn’t find an assessment to show. Retake the assessment to get your results.'
const LOAD_ERROR = 'We couldn’t load your results. Check your connection and try again.'

export default function ResultsPage() {
  const location = useLocation()
  const sessionId = (location.state as { sessionId?: string } | null)?.sessionId
  const [assessment, setAssessment] = useState<RiskAssessmentDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    if (!sessionId) {
      setLoading(false)
      setError(NO_SESSION_ERROR)
      return
    }

    const stored = readStoredResult(sessionId)
    if (stored && stored.riskLevel) {
      setAssessment({
        riskScore: stored.riskScore ?? 0,
        riskLevel: stored.riskLevel,
        recommendations: stored.recommendations ?? [],
        createdAt: new Date().toISOString(),
      })
      setLoading(false)
      return
    }

    setLoading(true)
    setError(null)
    apiFetch<LatestResultResponse>(`/api/results/latest?sessionId=${encodeURIComponent(sessionId)}`)
      .then((response) => {
        setAssessment(response.assessment)
        setLoading(false)
      })
      .catch(() => {
        setError(LOAD_ERROR)
        setLoading(false)
      })
  }, [sessionId, attempt])

  if (loading) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-12" aria-busy="true">
        <span className="sr-only">Loading your results…</span>
        <Skeleton className="mx-auto h-8 w-32" />
        <Skeleton className="mx-auto mt-4 h-16 w-40" />
        <div className="mt-6 space-y-3">
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-full" />
          <Skeleton className="h-4 w-3/4" />
        </div>
      </main>
    )
  }

  if (error || !assessment) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center">
        <div className="inline-flex text-left">
          <StatusMessage tone="error">{error ?? NO_SESSION_ERROR}</StatusMessage>
        </div>
        <div className="mt-6 flex flex-col items-center justify-center gap-3 sm:flex-row">
          {error === LOAD_ERROR && (
            <Button variant="secondary" onClick={() => setAttempt((a) => a + 1)}>
              Try again
            </Button>
          )}
          <LinkButton href="/app/questionnaire" variant="primary">
            Retake the assessment
          </LinkButton>
        </div>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-12">
      <Reveal>
        <Card className="p-8 text-center sm:p-10">
          <Badge tone={riskTone[assessment.riskLevel]}>{assessment.riskLevel} risk</Badge>
          <p className="mt-5 font-display text-6xl text-ink">{assessment.riskScore}</p>
          <p className="mt-1 text-sm text-ink-soft">out of 100</p>
        </Card>
      </Reveal>

      <Card className="mt-6 p-8">
        <h2 className="text-lg text-ink">What this means for you</h2>
        <ul className="mt-4 space-y-3">
          {assessment.recommendations.map((rec) => (
            <li key={rec} className="flex gap-3 text-ink-soft">
              <span aria-hidden className="mt-1 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-teal-500" />
              <span>{rec}</span>
            </li>
          ))}
        </ul>
      </Card>

      <div className="mt-8 flex flex-col gap-3 sm:flex-row">
        <LinkButton href="/app/health-centers" variant="primary" className="flex-1">
          Find a health center
        </LinkButton>
        <LinkButton href="/app/questionnaire" variant="secondary" className="flex-1">
          Retake assessment
        </LinkButton>
      </div>
    </main>
  )
}
