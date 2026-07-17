import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { Badge } from '../../components/Badge'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { apiFetch, apiPost } from '../../lib/api-client'
import type { UsersResponse } from './types'

const LOAD_ERROR = 'We couldn’t load the user list. Check your connection and try again.'
const TOGGLE_ERROR = 'We couldn’t update that user’s status. Check your connection and try again.'
const ROLE_ERROR = 'We couldn’t update that user’s role. Check your connection and try again.'

export default function AdminUsersPage() {
  const [data, setData] = useState<UsersResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [pendingId, setPendingId] = useState<string | null>(null)

  function load() {
    setError(null)
    apiFetch<UsersResponse>('/api/admin/users')
      .then(setData)
      .catch(() => setError(LOAD_ERROR))
  }

  useEffect(load, [])

  async function toggleStatus(userId: string, name: string, enabling: boolean) {
    setPendingId(userId)
    setError(null)
    setSuccess(null)
    try {
      await apiPost('/api/admin/users/toggle-status', { userId: Number(userId) })
      setSuccess(`${name} was ${enabling ? 'enabled' : 'disabled'}.`)
      load()
    } catch {
      setError(TOGGLE_ERROR)
    } finally {
      setPendingId(null)
    }
  }

  async function changeRole(userId: string, name: string, role: 'USER' | 'ADMIN') {
    setPendingId(userId)
    setError(null)
    setSuccess(null)
    try {
      await apiPost('/api/admin/users/role', { userId: Number(userId), role })
      setSuccess(`${name} is now ${role === 'ADMIN' ? 'an admin' : 'a regular user'}.`)
      load()
    } catch (err) {
      setError(err instanceof Error ? err.message : ROLE_ERROR)
    } finally {
      setPendingId(null)
    }
  }

  return (
    <main className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="font-display text-2xl text-ink">User management</h1>
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
      {success && (
        <div className="mt-4">
          <StatusMessage tone="success">{success}</StatusMessage>
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
          <p className="text-ink-soft">
            {data.totalUsers} total users &middot; {data.adminUsersCount} admins
          </p>

          <div className="mt-4 hidden overflow-x-auto sm:block">
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
                {data.users.map((user, i) => (
                  <tr
                    key={user.id}
                    className={`border-b border-teal-50 transition-colors hover:bg-teal-50/60 ${
                      i % 2 === 1 ? 'bg-paper-dim/40' : ''
                    }`}
                  >
                    <td className="py-2 text-ink">{user.name}</td>
                    <td className="py-2 text-ink-soft">{user.email}</td>
                    <td className="py-2">
                      <select
                        aria-label={`Role for ${user.name}`}
                        value={user.role}
                        disabled={pendingId === user.id}
                        onChange={(e) => changeRole(user.id, user.name, e.target.value as 'USER' | 'ADMIN')}
                        className="rounded-lg border border-teal-100 bg-paper px-2 py-1 text-sm text-ink-soft disabled:opacity-50"
                      >
                        <option value="USER">User</option>
                        <option value="ADMIN">Admin</option>
                      </select>
                    </td>
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
                        onClick={() => toggleStatus(user.id, user.name, !user.enabled)}
                      >
                        {user.enabled ? 'Disable' : 'Enable'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="mt-4 space-y-3 sm:hidden">
            {data.users.map((user) => (
              <Card key={user.id} className="p-4">
                <div className="flex items-center justify-between gap-2">
                  <p className="font-medium text-ink">{user.name}</p>
                  <Badge tone={user.enabled ? 'low' : 'neutral'}>{user.enabled ? 'Active' : 'Disabled'}</Badge>
                </div>
                <p className="mt-1 text-sm text-ink-soft">{user.email}</p>
                <div className="mt-2 flex items-center gap-2">
                  <label className="text-sm text-ink-soft" htmlFor={`role-${user.id}`}>
                    Role
                  </label>
                  <select
                    id={`role-${user.id}`}
                    value={user.role}
                    disabled={pendingId === user.id}
                    onChange={(e) => changeRole(user.id, user.name, e.target.value as 'USER' | 'ADMIN')}
                    className="rounded-lg border border-teal-100 bg-paper px-2 py-1 text-sm text-ink-soft disabled:opacity-50"
                  >
                    <option value="USER">User</option>
                    <option value="ADMIN">Admin</option>
                  </select>
                </div>
                <p className="mt-1 text-sm text-ink-soft">{user.questionnaireCount} assessments</p>
                <Button
                  variant="ghost"
                  className="mt-2"
                  disabled={pendingId === user.id}
                  onClick={() => toggleStatus(user.id, user.name, !user.enabled)}
                >
                  {user.enabled ? 'Disable' : 'Enable'}
                </Button>
              </Card>
            ))}
          </div>
        </>
      )}
    </main>
  )
}
