import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { apiFetch } from '../../lib/api-client'
import type { AuditLogResponse } from './types'

const LOAD_ERROR = 'We couldn’t load the audit log. Check your connection and try again.'

// Access-denied entries (non-admins probing /api/admin/**, see AuditingAccessDeniedHandler)
// are the one action type worth calling out visually — everything else is routine admin
// activity, this is the one that might mean something's wrong.
function actionTone(action: string): 'high' | 'neutral' {
  return action === 'ACCESS_DENIED' ? 'high' : 'neutral'
}

export default function AdminAuditLogPage() {
  const [data, setData] = useState<AuditLogResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  function load() {
    setError(null)
    apiFetch<AuditLogResponse>('/api/admin/audit-log')
      .then(setData)
      .catch(() => setError(LOAD_ERROR))
  }

  useEffect(load, [])

  return (
    <main className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="font-display text-2xl text-ink">Audit log</h1>
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
          {Array.from({ length: 6 }).map((_, i) => (
            <Skeleton key={i} className="h-12" />
          ))}
        </div>
      )}

      {data && (
        <>
          <p className="text-ink-soft">Most recent {data.entries.length} actions.</p>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-teal-100 text-ink-soft">
                  <th scope="col" className="py-2">
                    When
                  </th>
                  <th scope="col" className="py-2">
                    Actor
                  </th>
                  <th scope="col" className="py-2">
                    Action
                  </th>
                  <th scope="col" className="py-2">
                    Target
                  </th>
                  <th scope="col" className="py-2">
                    Details
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.entries.map((entry, i) => (
                  <tr
                    key={entry.id}
                    className={`border-b border-teal-50 ${i % 2 === 1 ? 'bg-paper-dim/40' : ''}`}
                  >
                    <td className="whitespace-nowrap py-2 text-ink-soft">
                      {new Date(entry.createdAt).toLocaleString()}
                    </td>
                    <td className="py-2 text-ink-soft">{entry.actorEmail}</td>
                    <td className="py-2">
                      <Badge tone={actionTone(entry.action)}>{entry.action}</Badge>
                    </td>
                    <td className="py-2 text-ink-soft">
                      {entry.targetType && entry.targetId ? `${entry.targetType} ${entry.targetId}` : '—'}
                    </td>
                    <td className="py-2 text-ink-soft">{entry.details ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {data.entries.length === 0 && <p className="mt-4 text-ink-soft">No admin activity recorded yet.</p>}
        </>
      )}
    </main>
  )
}
