import { RequireAuth } from '../../lib/auth/RequireAuth'
import AdminAdminsPage from './AdminAdminsPage'

export function Component() {
  return (
    <RequireAuth adminOnly>
      <AdminAdminsPage />
    </RequireAuth>
  )
}
