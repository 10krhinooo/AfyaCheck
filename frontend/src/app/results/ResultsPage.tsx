import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { Badge } from '../../components/Badge'
import { LinkButton } from '../../components/Button'
import { Card } from '../../components/Card'
import { apiFetch } from '../../lib/api-client'
import type { LatestResultResponse, RiskAssessmentDto } from '../../lib/results-types'
import { readStoredResult } from './resultStorage'

const riskTone = { Low: 'low', Medium: 'moderate', High: 'high' } as const

export default function ResultsPage() {
  const location = useLocation()
  const sessionId = (location.state as { sessionId?: string } | null)?.sessionId
  const [assessment, setAssessment] = useState<RiskAssessmentDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!sessionId) {
      setLoading(false)
      setError('No assessment session found. Please retake the assessment.')
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

    apiFetch<LatestResultResponse>(`/api/results/latest?sessionId=${encodeURIComponent(sessionId)}`)
      .then((response) => {
        setAssessment(response.assessment)
        setLoading(false)
      })
      .catch((err: Error) => {
        setError(err.message)
        setLoading(false)
      })
  }, [sessionId])

  if (loading) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-16 text-center text-ink-soft" aria-busy="true">
        Loading your results…
      </main>
    )
  }

  if (error || !assessment) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center">
        <p className="text-coral-700">{error ?? 'We couldn’t find your results.'}</p>
        <LinkButton href="/app/questionnaire" variant="primary" className="mt-6">
          Retake the assessment
        </LinkButton>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-12">
      <Card className="p-8 text-center">
        <Badge tone={riskTone[assessment.riskLevel]}>{assessment.riskLevel} risk</Badge>
        <p className="mt-4 font-display text-5xl text-ink">{assessment.riskScore}</p>
        <p className="mt-1 text-sm text-ink-soft">out of 100</p>
      </Card>

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
