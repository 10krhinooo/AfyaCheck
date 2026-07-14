import { useEffect, useState } from 'react'
import { apiClient } from '../../api/client'

interface AdminUser {
  id: string
  name: string
  email: string
  role: string
  enabled: boolean
  questionnaireCount: number
}

export function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUser[]>([])
  const [promoteEmail, setPromoteEmail] = useState('')
  const [message, setMessage] = useState<string | null>(null)

  function loadUsers() {
    apiClient.get<AdminUser[]>('/admin/users').then((response) => setUsers(response.data))
  }

  useEffect(loadUsers, [])

  async function toggleStatus(id: string) {
    await apiClient.post(`/admin/users/${id}/toggle-status`)
    loadUsers()
  }

  async function makeAdmin(event: React.FormEvent) {
    event.preventDefault()
    setMessage(null)
    try {
      const response = await apiClient.post('/admin/make-admin', { email: promoteEmail })
      setMessage(response.data.message ?? 'User promoted to admin.')
      setPromoteEmail('')
      loadUsers()
    } catch {
      setMessage('Failed to promote user.')
    }
  }

  return (
    <div className="container py-4">
      <h1 className="h3 mb-4">User Management</h1>

      <form className="row g-2 align-items-end mb-4" onSubmit={makeAdmin}>
        <div className="col-auto">
          <label className="form-label">Promote to admin (by email)</label>
          <input
            className="form-control"
            type="email"
            value={promoteEmail}
            onChange={(e) => setPromoteEmail(e.target.value)}
            required
          />
        </div>
        <div className="col-auto">
          <button type="submit" className="btn btn-success">
            Promote
          </button>
        </div>
      </form>

      {message && <div className="alert alert-info">{message}</div>}

      <table className="table">
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Assessments</th>
            <th>Status</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id}>
              <td>{user.name}</td>
              <td>{user.email}</td>
              <td>{user.role}</td>
              <td>{user.questionnaireCount}</td>
              <td>
                <span className={`badge ${user.enabled ? 'bg-success' : 'bg-secondary'}`}>
                  {user.enabled ? 'Active' : 'Disabled'}
                </span>
              </td>
              <td>
                <button className="btn btn-sm btn-outline-secondary" onClick={() => toggleStatus(user.id)}>
                  {user.enabled ? 'Disable' : 'Enable'}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
