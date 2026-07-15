import { Outlet } from 'react-router-dom'
import { AuthProvider } from './lib/auth/AuthContext'

export default function Layout() {
  return (
    <AuthProvider>
      <Outlet />
    </AuthProvider>
  )
}
