import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Navigate } from 'react-router-dom'

export function LandingPage() {
  const { user, isLoading } = useAuth()

  if (!isLoading && user) {
    return <Navigate to="/dashboard" replace />
  }

  return (
    <div className="container py-5 text-center">
      <h1 className="display-5 fw-bold mb-3">AfyaCheck</h1>
      <p className="lead text-muted mb-4">
        Take a quick, confidential STI risk assessment and get personalized
        recommendations and nearby health center suggestions.
      </p>
      <div className="d-flex justify-content-center gap-3">
        <Link to="/login" className="btn btn-success btn-lg">
          Log in
        </Link>
        <Link to="/register" className="btn btn-outline-success btn-lg">
          Create an account
        </Link>
      </div>
    </div>
  )
}
