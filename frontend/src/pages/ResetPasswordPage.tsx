import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import axios from 'axios'
import { apiClient } from '../api/client'
import type { ApiErrorBody } from '../api/types'

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''
  const navigate = useNavigate()
  const [newPassword, setNewPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setIsSubmitting(true)
    try {
      await apiClient.post('/auth/reset-password', { token, newPassword })
      navigate('/login?resetSuccess=true')
    } catch (err) {
      if (axios.isAxiosError<ApiErrorBody>(err) && err.response?.data?.error) {
        setError(err.response.data.error)
      } else {
        setError('Failed to reset password. The link may have expired.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!token) {
    return (
      <div className="container py-5 text-center" style={{ maxWidth: 420 }}>
        <div className="alert alert-danger">This reset link is missing a token.</div>
        <Link to="/forgot-password">Request a new link</Link>
      </div>
    )
  }

  return (
    <div className="container py-5" style={{ maxWidth: 420 }}>
      <h1 className="h3 mb-4 text-center">Choose a new password</h1>

      {error && <div className="alert alert-danger">{error}</div>}

      <form onSubmit={handleSubmit}>
        <div className="mb-3">
          <label className="form-label" htmlFor="newPassword">
            New password
          </label>
          <input
            id="newPassword"
            type="password"
            className="form-control"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            minLength={8}
            required
          />
          <div className="form-text">At least 8 characters.</div>
        </div>
        <button type="submit" className="btn btn-success w-100" disabled={isSubmitting}>
          {isSubmitting ? 'Resetting...' : 'Reset password'}
        </button>
      </form>
    </div>
  )
}
