import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'

interface SessionSummary {
  sessionId: string
  status: string
  riskScore: number | null
  createdAt: string
}

export function ResultsHistoryPage() {
  const [sessions, setSessions] = useState<SessionSummary[] | null>(null)

  useEffect(() => {
    apiClient.get<SessionSummary[]>('/sessions').then((response) => setSessions(response.data))
  }, [])

  return (
    <div className="container py-5" style={{ maxWidth: 700 }}>
      <h1 className="h3 mb-4">My Assessment History</h1>

      {sessions === null && <div className="page-loading">Loading...</div>}

      {sessions !== null && sessions.length === 0 && (
        <div className="text-center py-5">
          <p className="text-muted">You haven't completed an assessment yet.</p>
          <Link to="/questionnaire/start" className="btn btn-success">
            Start Your First Assessment
          </Link>
        </div>
      )}

      {sessions !== null && sessions.length > 0 && (
        <div className="list-group">
          {sessions.map((session) => (
            <Link
              key={session.sessionId}
              to={`/results/${session.sessionId}`}
              className="list-group-item list-group-item-action d-flex justify-content-between align-items-center"
            >
              <div>
                <div>{new Date(session.createdAt).toLocaleDateString()}</div>
                <small className="text-muted">Status: {session.status}</small>
              </div>
              {session.riskScore !== null && (
                <span className="badge bg-success rounded-pill">{session.riskScore}%</span>
              )}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
