import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { apiClient } from '../api/client'

interface ResultData {
  sessionId: string
  riskScore: number
  riskLevel: string
  recommendations: string[]
  createdAt: string
}

export function ResultsPage() {
  const { sessionId } = useParams<{ sessionId: string }>()
  const [result, setResult] = useState<ResultData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    if (!sessionId) return
    apiClient
      .get<ResultData>(`/results/${sessionId}`)
      .then((response) => setResult(response.data))
      .catch(() => setError('These results could not be found, or you do not have permission to view them.'))
      .finally(() => setIsLoading(false))
  }, [sessionId])

  if (isLoading) {
    return <div className="page-loading text-center py-5">Loading...</div>
  }

  if (error || !result) {
    return (
      <div className="container py-5 text-center">
        <div className="alert alert-danger">{error}</div>
        <Link to="/dashboard">Back to Dashboard</Link>
      </div>
    )
  }

  const riskClass =
    result.riskLevel === 'Low' ? 'success' : result.riskLevel === 'Medium' ? 'warning' : 'danger'

  return (
    <div className="container py-5" style={{ maxWidth: 700 }}>
      <h1 className="h3 text-center mb-4">STI Risk Assessment Results</h1>

      <div className="text-center mb-4">
        <div className="display-4 fw-bold text-success">{result.riskScore}%</div>
        <span className={`badge bg-${riskClass} fs-6`}>{result.riskLevel} Risk Level</span>
      </div>

      <div className="card mb-4">
        <div className="card-body">
          <h2 className="h5 card-title">Recommendations</h2>
          <ul>
            {result.recommendations.map((rec, index) => (
              <li key={index}>{rec}</li>
            ))}
          </ul>
        </div>
      </div>

      <div className="d-flex justify-content-center gap-3">
        <Link to="/questionnaire/start" className="btn btn-success">
          Begin New Assessment
        </Link>
        <Link to="/health-centers" className="btn btn-outline-success">
          Find Health Centers
        </Link>
      </div>
    </div>
  )
}
