import { RequireAuth } from '../../lib/auth/RequireAuth'
import AdminUsersPage from './AdminUsersPage'

export function Component() {
  return (
    <RequireAuth adminOnly>
      <AdminUsersPage />
    </RequireAuth>
  )
}
