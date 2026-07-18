import { RequireAuth } from '../../lib/auth/RequireAuth'
import AdminAuditLogPage from './AdminAuditLogPage'

export function Component() {
  return (
    <RequireAuth adminOnly>
      <AdminAuditLogPage />
    </RequireAuth>
  )
}
