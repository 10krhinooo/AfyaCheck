import { useState } from 'react'
import { Menu, X, LogOut } from 'lucide-react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/auth/AuthContext'
import { login, logout } from '../lib/auth/keycloak'
import { StatusMessage } from './StatusMessage'

const baseLinks = [
  { to: '/', label: 'Home', end: true },
  { to: '/app/questionnaire', label: 'Assessment' },
  { to: '/app/health-centers', label: 'Health Centers' },
  { to: '/about', label: 'About' },
  { to: '/faq', label: 'FAQ' },
]

function NavItem({
  to,
  label,
  end,
  onClick,
}: {
  to: string
  label: string
  end?: boolean
  onClick?: () => void
}) {
  return (
    <NavLink
      to={to}
      end={end}
      onClick={onClick}
      className={({ isActive }) =>
        `text-sm font-medium transition-colors ${isActive ? 'text-teal-700' : 'text-ink-soft hover:text-ink'}`
      }
    >
      {label}
    </NavLink>
  )
}

// Single shared masthead for every route (Landing included), mounted once in Layout.tsx.
// Logo is always the grid's first (auto-width) column so it stays flush-left regardless of
// how many nav links render either side of it — not just visually left-leaning by luck of
// justify-between with a variable number of children.
export function NavBar() {
  const { pathname } = useLocation()
  const { isAuthenticated, isAdmin } = useAuth()
  const [open, setOpen] = useState(false)
  const [authError, setAuthError] = useState<string | null>(null)

  // Silent OIDC redirect screen — no chrome worth showing (see routes.tsx).
  if (pathname === '/app/callback') return null

  const links = [
    ...baseLinks,
    ...(isAuthenticated ? [{ to: '/app/dashboard', label: 'Dashboard' }] : []),
    ...(isAdmin ? [{ to: '/app/admin', label: 'Admin' }] : []),
  ]

  async function handleSignIn() {
    setAuthError(null)
    try {
      await login('/app/dashboard')
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Sign-in failed. Please try again.')
    }
  }

  async function handleSignOut() {
    setAuthError(null)
    try {
      await logout()
    } catch (err) {
      setAuthError(err instanceof Error ? err.message : 'Sign-out failed. Please try again.')
    }
  }

  return (
    <header className="border-b border-teal-100/60">
      <div className="mx-auto grid max-w-7xl grid-cols-[auto_1fr_auto] items-center gap-4 px-6 py-5">
        <a href="/" className="font-display text-xl text-teal-700">
          AfyaCheck
        </a>

        <nav aria-label="Primary" className="hidden justify-self-center md:flex md:items-center md:gap-6">
          {links.map((link) => (
            <NavItem key={link.to} to={link.to} label={link.label} end={link.end} />
          ))}
        </nav>

        <div className="hidden justify-self-end md:block">
          {isAuthenticated ? (
            <button
              type="button"
              onClick={handleSignOut}
              className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-soft hover:text-ink"
            >
              <LogOut aria-hidden="true" size={16} />
              Sign out
            </button>
          ) : (
            <button
              type="button"
              onClick={handleSignIn}
              className="text-sm font-medium text-ink-soft hover:text-ink"
            >
              Sign in
            </button>
          )}
        </div>

        <button
          type="button"
          onClick={() => setOpen((o) => !o)}
          aria-expanded={open}
          aria-label={open ? 'Close menu' : 'Open menu'}
          className="col-start-3 justify-self-end text-ink-soft hover:text-ink md:hidden"
        >
          {open ? <X aria-hidden="true" size={22} /> : <Menu aria-hidden="true" size={22} />}
        </button>
      </div>

      {open && (
        <nav aria-label="Primary" className="border-t border-teal-100/60 px-6 py-4 md:hidden">
          <div className="flex flex-col gap-4">
            {links.map((link) => (
              <NavItem
                key={link.to}
                to={link.to}
                label={link.label}
                end={link.end}
                onClick={() => setOpen(false)}
              />
            ))}
            {isAuthenticated ? (
              <button
                type="button"
                onClick={handleSignOut}
                className="inline-flex items-center gap-1.5 text-left text-sm font-medium text-ink-soft hover:text-ink"
              >
                <LogOut aria-hidden="true" size={16} />
                Sign out
              </button>
            ) : (
              <button
                type="button"
                onClick={() => {
                  setOpen(false)
                  handleSignIn()
                }}
                className="text-left text-sm font-medium text-ink-soft hover:text-ink"
              >
                Sign in
              </button>
            )}
          </div>
        </nav>
      )}

      {authError && (
        <div className="border-t border-teal-100/60 px-6 py-3">
          <StatusMessage tone="error">{authError}</StatusMessage>
        </div>
      )}
    </header>
  )
}
