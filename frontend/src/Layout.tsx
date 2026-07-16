import { Outlet } from 'react-router-dom'
import { AuthProvider } from './lib/auth/AuthContext'
import { NavBar } from './components/NavBar'
import { Footer } from './components/Footer'

export default function Layout() {
  return (
    <AuthProvider>
      <div className="flex min-h-screen flex-col">
        <NavBar />
        <div className="flex-1">
          <Outlet />
        </div>
        <Footer />
      </div>
    </AuthProvider>
  )
}
