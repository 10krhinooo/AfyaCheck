import { Outlet } from 'react-router-dom'
import { AuthProvider } from './lib/auth/AuthContext'
import { I18nProvider } from './lib/i18n'
import { NavBar } from './components/NavBar'
import { Footer } from './components/Footer'

export default function Layout() {
  return (
    <I18nProvider>
      <AuthProvider>
        <div className="flex min-h-screen flex-col">
          <NavBar />
          <div className="flex-1">
            <Outlet />
          </div>
          <Footer />
        </div>
      </AuthProvider>
    </I18nProvider>
  )
}
