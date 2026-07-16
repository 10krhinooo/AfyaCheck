import { LogOut } from 'lucide-react'
import { useAuth } from '../lib/auth/AuthContext'
import { logout } from '../lib/auth/keycloak'

// Slim masthead shared by every /app/** route, just the wordmark plus, when
// authenticated, a single "Sign out" action. No nav links, no hamburger: the
// questionnaire/results/health-centers flow stays as focused as it is today,
// and Dashboard/Admin get the one action they actually need.
export function AppHeader() {
  const { isAuthenticated } = useAuth()

  return (
    <header className="mx-auto flex max-w-5xl items-center justify-between px-6 py-6">
      <a href="/" className="font-display text-xl text-teal-700">
        AfyaCheck
      </a>
      {isAuthenticated && (
        <button
          type="button"
          onClick={() => logout()}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-soft hover:text-ink"
        >
          <LogOut aria-hidden="true" size={16} />
          Sign out
        </button>
      )}
    </header>
  )
}
