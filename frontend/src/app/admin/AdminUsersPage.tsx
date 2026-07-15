import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { apiFetch, apiPost } from '../../lib/api-client'
import type { UsersResponse } from './types'

export default function AdminUsersPage() {
  const [data, setData] = useState<UsersResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [pendingId, setPendingId] = useState<string | null>(null)

  function load() {
    apiFetch<UsersResponse>('/api/admin/users')
      .then(setData)
      .catch((err: Error) => setError(err.message))
  }

  useEffect(load, [])

  async function toggleStatus(userId: string) {
    setPendingId(userId);
    try {
      await apiPost('/api/admin/users/toggle-status', { userId: Number(userId) })
      load()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setPendingId(null)
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="text-2xl text-ink">User management</h1>
      <AdminNav />

      {error && <p className="text-coral-700">{error}</p>}

      {data && (
        <>
          <p className="text-ink-soft">
            {data.totalUsers} total users &middot; {data.adminUsersCount} admins
          </p>

          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead>
                <tr className="border-b border-teal-100 text-ink-soft">
                  <th scope="col" className="py-2">
                    Name
                  </th>
                  <th scope="col" className="py-2">
                    Email
                  </th>
                  <th scope="col" className="py-2">
                    Role
                  </th>
                  <th scope="col" className="py-2">
                    Status
                  </th>
                  <th scope="col" className="py-2">
                    Assessments
                  </th>
                  <th scope="col" className="py-2">
                    <span className="sr-only">Actions</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {data.users.map((user) => (
                  <tr key={user.id} className="border-b border-teal-50">
                    <td className="py-2 text-ink">{user.name}</td>
                    <td className="py-2 text-ink-soft">{user.email}</td>
                    <td className="py-2 text-ink-soft">{user.role}</td>
                    <td className="py-2">
                      <Badge tone={user.enabled ? 'low' : 'neutral'}>
                        {user.enabled ? 'Active' : 'Disabled'}
                      </Badge>
                    </td>
                    <td className="py-2 text-ink-soft">{user.questionnaireCount}</td>
                    <td className="py-2 text-right">
                      <Button
                        variant="ghost"
                        disabled={pendingId === user.id}
                        onClick={() => toggleStatus(user.id)}
                      >
                        {user.enabled ? 'Disable' : 'Enable'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}
    </main>
  )
}
