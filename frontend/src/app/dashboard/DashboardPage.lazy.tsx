import { RequireAuth } from '../../lib/auth/RequireAuth'
import DashboardPage from './DashboardPage'

export function Component() {
  return (
    <RequireAuth>
      <DashboardPage />
    </RequireAuth>
  )
}
