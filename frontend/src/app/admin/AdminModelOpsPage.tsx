import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { apiFetch } from '../../lib/api-client'
import { StatTile } from './StatTile'
import type { ModelOpsResponse } from './types'

const LOAD_ERROR = 'We couldn’t load model ops data. Check your connection and try again.'

function HealthBadge({ healthy, label }: { healthy: boolean; label: string }) {
  return (
    <span className="flex items-center gap-2">
      <span className="text-sm text-ink-soft">{label}</span>
      <Badge tone={healthy ? 'low' : 'high'}>{healthy ? 'Healthy' : 'Down'}</Badge>
    </span>
  )
}

export default function AdminModelOpsPage() {
  const [data, setData] = useState<ModelOpsResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  function load() {
    setError(null)
    apiFetch<ModelOpsResponse>('/api/admin/model-ops')
      .then(setData)
      .catch(() => setError(LOAD_ERROR))
  }

  useEffect(load, [])

  const maxDaily = data ? Math.max(1, ...data.assessmentsPerDay.map((d) => d.count)) : 1

  return (
    <main className="w-full px-6 py-10">
      <h1 className="font-display text-2xl text-ink">Model ops</h1>
      <AdminNav />

      {error && (
        <div className="mt-4">
          <StatusMessage
            tone="error"
            action={
              <Button variant="secondary" onClick={load}>
                Try again
              </Button>
            }
          >
            {error}
          </StatusMessage>
        </div>
      )}

      {!data && !error && (
        <div aria-busy="true" className="mt-4 space-y-3">
          <span className="sr-only">Loading…</span>
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-24" />
          ))}
        </div>
      )}

      {data && (
        <>
          <div className="flex flex-wrap items-center gap-6">
            <HealthBadge healthy={data.mlServiceHealthy} label="ML risk service" />
            <HealthBadge healthy={data.decisionTreeServiceHealthy} label="Decision tree service" />
          </div>

          <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatTile label="Total assessments" value={data.totalAssessments} />
            {Object.entries(data.riskLevelCounts).map(([level, count]) => (
              <StatTile key={level} label={`${level} risk`} value={count} />
            ))}
          </div>

          <Card className="mt-6 p-6">
            <h2 className="text-lg text-ink">Assessments by model version</h2>
            <p className="mt-1 text-sm text-ink-soft">
              Every stored assessment is tagged with the model version that produced it; fallback paths tag
              themselves with a distinct <code>-fallback-</code> marker.
            </p>
            {data.modelVersions.length === 0 ? (
              <p className="mt-4 text-ink-soft">No assessments recorded yet.</p>
            ) : (
              <div className="mt-4 overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead>
                    <tr className="border-b border-teal-100 text-ink-soft">
                      <th scope="col" className="py-2">
                        Model version
                      </th>
                      <th scope="col" className="py-2">
                        Assessments
                      </th>
                      <th scope="col" className="py-2">
                        Avg risk score
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.modelVersions.map((version, i) => (
                      <tr
                        key={version.modelVersion}
                        className={`border-b border-teal-50 ${i % 2 === 1 ? 'bg-paper-dim/40' : ''}`}
                      >
                        <td className="py-2 font-mono text-ink">{version.modelVersion}</td>
                        <td className="py-2 text-ink-soft">{version.count}</td>
                        <td className="py-2 text-ink-soft">{version.avgRiskScore ?? 'None'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>

          <Card className="mt-6 p-6">
            <h2 className="text-lg text-ink">Assessments per day (last 30 days)</h2>
            {data.assessmentsPerDay.length === 0 ? (
              <p className="mt-4 text-ink-soft">No assessments in the last 30 days.</p>
            ) : (
              <ul className="mt-4 space-y-2">
                {data.assessmentsPerDay.map((day) => (
                  <li key={day.date} className="flex items-center gap-3 text-sm">
                    <span className="w-28 shrink-0 whitespace-nowrap text-ink-soft">{day.date}</span>
                    <span
                      aria-hidden
                      className="h-3 rounded-sm bg-teal-500"
                      style={{ width: `${Math.max(2, (day.count / maxDaily) * 100)}%` }}
                    />
                    <span className="text-ink">{day.count}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>

        </>
      )}
    </main>
  )
}
