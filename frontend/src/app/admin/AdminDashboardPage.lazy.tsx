import { RequireAuth } from '../../lib/auth/RequireAuth'
import AdminDashboardPage from './AdminDashboardPage'

export function Component() {
  return (
    <RequireAuth adminOnly>
      <AdminDashboardPage />
    </RequireAuth>
  )
}
