import { RequireAuth } from '../../lib/auth/RequireAuth'
import AdminHealthCentersPage from './AdminHealthCentersPage'

export function Component() {
  return (
    <RequireAuth adminOnly>
      <AdminHealthCentersPage />
    </RequireAuth>
  )
}
