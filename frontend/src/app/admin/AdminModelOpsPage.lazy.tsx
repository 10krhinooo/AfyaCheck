import { RequireAuth } from '../../lib/auth/RequireAuth'
import AdminModelOpsPage from './AdminModelOpsPage'

export function Component() {
  return (
    <RequireAuth adminOnly>
      <AdminModelOpsPage />
    </RequireAuth>
  )
}
