import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { Badge } from '../../components/Badge'
import { Button, LinkButton } from '../../components/Button'
import { Card } from '../../components/Card'
import { Reveal } from '../../components/Reveal'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { apiFetch } from '../../lib/api-client'
import { useTranslation } from '../../lib/i18n'
import type { HistoryResponse, LatestResultResponse, RiskAssessmentDto } from '../../lib/results-types'
import { DataControls } from './DataControls'
import { EmailResultsForm } from './EmailResultsForm'
import { RemindMeForm } from './RemindMeForm'
import { readLastSessionId, readStoredResult } from './resultStorage'

const riskTone = { Low: 'low', Medium: 'moderate', High: 'high' } as const
const riskLabelKey = { Low: 'results.riskLow', Medium: 'results.riskMedium', High: 'results.riskHigh' } as const
const NO_SESSION_ERROR = 'results.noSession'
const LOAD_ERROR = 'results.loadError'

export default function ResultsPage() {
  const location = useLocation()
  const { t } = useTranslation()
  // Router state on direct navigation; sessionStorage fallback so a page refresh (which
  // loses router state) still shows the assessment from this browser session.
  const sessionId = (location.state as { sessionId?: string } | null)?.sessionId ?? readLastSessionId() ?? undefined
  const [assessment, setAssessment] = useState<RiskAssessmentDto | null>(null)
  const [history, setHistory] = useState<RiskAssessmentDto[]>([])
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

  // Best-effort: earlier assessments from the same session, shown below the current one.
  // Failures here never block the main result.
  useEffect(() => {
    if (!sessionId) return
    apiFetch<HistoryResponse>(`/api/results/history?sessionId=${encodeURIComponent(sessionId)}`)
      .then((response) => setHistory(response.assessments ?? []))
      .catch(() => setHistory([]))
  }, [sessionId, attempt])

  if (loading) {
    return (
      <main className="mx-auto max-w-2xl px-6 py-12" aria-busy="true">
        <span className="sr-only">{t('results.loading')}</span>
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
          <StatusMessage tone="error">{t(error ?? NO_SESSION_ERROR)}</StatusMessage>
        </div>
        <div className="mt-6 flex flex-col items-center justify-center gap-3 sm:flex-row">
          {error === LOAD_ERROR && (
            <Button variant="secondary" onClick={() => setAttempt((a) => a + 1)}>
              {t('results.tryAgain')}
            </Button>
          )}
          <LinkButton href="/app/questionnaire" variant="primary">
            {t('results.retakeCta')}
          </LinkButton>
        </div>
      </main>
    )
  }

  return (
    <main className="mx-auto max-w-2xl px-6 py-12">
      <Reveal>
        <Card className="p-8 text-center sm:p-10">
          <Badge tone={riskTone[assessment.riskLevel]}>{t(riskLabelKey[assessment.riskLevel])}</Badge>
          <p className="mt-5 font-display text-6xl text-ink">{assessment.riskScore}</p>
          <p className="mt-1 text-sm text-ink-soft">{t('results.outOf')}</p>
        </Card>
      </Reveal>

      <Card className="mt-6 p-8">
        <h2 className="text-lg text-ink">{t('results.whatThisMeans')}</h2>
        <ul className="mt-4 space-y-3">
          {assessment.recommendations.map((rec) => (
            <li key={rec} className="flex gap-3 text-ink-soft">
              <span aria-hidden className="mt-1 h-1.5 w-1.5 flex-shrink-0 rounded-full bg-teal-500" />
              <span>{rec}</span>
            </li>
          ))}
        </ul>
      </Card>

      {history.length > 1 && (
        <Card className="mt-6 p-8">
          <h2 className="text-lg text-ink">{t('results.history')}</h2>
          <ul className="mt-4 divide-y divide-teal-50">
            {history.slice(1).map((item, index) => (
              <li key={`${item.createdAt}-${index}`} className="flex items-center justify-between gap-4 py-3">
                <span className="text-sm text-ink-soft">
                  {new Date(item.createdAt).toLocaleDateString(undefined, {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric',
                  })}
                </span>
                <span className="flex items-center gap-3">
                  <Badge tone={riskTone[item.riskLevel]}>{t(riskLabelKey[item.riskLevel])}</Badge>
                  <span className="font-display text-lg text-ink">{item.riskScore}</span>
                </span>
              </li>
            ))}
          </ul>
        </Card>
      )}

      {sessionId && (
        <Card className="mt-6 p-8">
          <EmailResultsForm sessionId={sessionId} />
        </Card>
      )}

      <Card className="mt-6 p-8">
        <RemindMeForm />
      </Card>

      {sessionId && (
        <Card className="mt-6 p-8">
          <DataControls sessionId={sessionId} assessment={assessment} />
        </Card>
      )}

      <div className="mt-8 flex flex-col gap-3 sm:flex-row">
        <LinkButton href="/app/health-centers" variant="primary" className="flex-1">
          {t('results.findHealthCenter')}
        </LinkButton>
        <LinkButton href="/app/questionnaire" variant="secondary" className="flex-1">
          {t('results.retake')}
        </LinkButton>
      </div>
    </main>
  )
}
