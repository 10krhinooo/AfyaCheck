import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { apiClient } from '../api/client'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState<string | null>(null)
  const [isSubmitting, setIsSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setIsSubmitting(true)
    try {
      const response = await apiClient.post('/auth/forgot-password', { email })
      // The backend always returns the same message regardless of whether
      // the email exists, so there's nothing to branch on here.
      setMessage(response.data.message ?? 'If that email exists, a reset link has been sent.')
    } catch {
      setMessage('If that email exists, a reset link has been sent.')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="container py-5" style={{ maxWidth: 420 }}>
      <h1 className="h3 mb-4 text-center">Forgot your password?</h1>

      {message ? (
        <div className="alert alert-info">{message}</div>
      ) : (
        <form onSubmit={handleSubmit}>
          <div className="mb-3">
            <label className="form-label" htmlFor="email">
              Email
            </label>
            <input
              id="email"
              type="email"
              className="form-control"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <button type="submit" className="btn btn-success w-100" disabled={isSubmitting}>
            {isSubmitting ? 'Sending...' : 'Send reset link'}
          </button>
        </form>
      )}

      <div className="text-center mt-4">
        <Link to="/login">Back to login</Link>
      </div>
    </div>
  )
}
