import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function DashboardPage() {
  const { user } = useAuth()

  return (
    <div className="container py-5">
      <h1 className="h3 mb-4">Welcome, {user?.name}</h1>

      <div className="row g-4">
        <div className="col-md-4">
          <div className="card h-100">
            <div className="card-body">
              <h5 className="card-title">Take an assessment</h5>
              <p className="card-text text-muted">
                Start a new confidential STI risk assessment.
              </p>
              <Link to="/questionnaire/start" className="btn btn-success">
                Start Assessment
              </Link>
            </div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card h-100">
            <div className="card-body">
              <h5 className="card-title">My assessments</h5>
              <p className="card-text text-muted">
                View your past assessment results and recommendations.
              </p>
              <Link to="/results/history" className="btn btn-outline-success">
                View History
              </Link>
            </div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="card h-100">
            <div className="card-body">
              <h5 className="card-title">Find health centers</h5>
              <p className="card-text text-muted">
                Locate nearby clinics, hospitals, and pharmacies.
              </p>
              <Link to="/health-centers" className="btn btn-outline-success">
                Find Centers
              </Link>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
