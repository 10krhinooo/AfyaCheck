import { RequireAuth } from '../../lib/auth/RequireAuth'
import AdminQuestionsPage from './AdminQuestionsPage'

export function Component() {
  return (
    <RequireAuth adminOnly>
      <AdminQuestionsPage />
    </RequireAuth>
  )
}
