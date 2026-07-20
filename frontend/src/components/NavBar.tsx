import { useState } from 'react'
import { Menu, X, LogOut } from 'lucide-react'
import { NavLink, useLocation } from 'react-router-dom'
import { useAuth } from '../lib/auth/AuthContext'
import { login, logout } from '../lib/auth/keycloak'
import { useTranslation } from '../lib/i18n'
import { LanguageSwitcher } from './LanguageSwitcher'
import { StatusMessage } from './StatusMessage'

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
// how many nav links render either side of it, not just visually left-leaning by luck of
// justify-between with a variable number of children.
export function NavBar() {
  const { pathname } = useLocation()
  const { isAuthenticated, isAdmin } = useAuth()
  const { t } = useTranslation()
  const [open, setOpen] = useState(false)
  const [authError, setAuthError] = useState<string | null>(null)

  // Silent OIDC redirect screen: no chrome worth showing (see routes.tsx).
  if (pathname === '/app/callback') return null

  const baseLinks = [
    { to: '/', label: t('nav.home'), end: true },
    { to: '/app/questionnaire', label: t('nav.assessment') },
    { to: '/app/health-centers', label: t('nav.healthCenters') },
    { to: '/learn', label: t('nav.learn') },
    { to: '/about', label: t('nav.about') },
    { to: '/faq', label: t('nav.faq') },
  ]

  // The admin console (AdminNav) has its own internal navigation (Dashboard/Admins/Questions/
  // Audit log): the public marketing links here are just clutter while managing the system,
  // so this masthead shrinks to just sign-out + the logo (rendered separately below).
  const isAdminRoute = pathname.startsWith('/app/admin')
  const links = isAdminRoute
    ? []
    : [
        ...baseLinks,
        ...(isAuthenticated ? [{ to: '/app/dashboard', label: t('nav.dashboard') }] : []),
        ...(isAdmin ? [{ to: '/app/admin', label: t('nav.admin') }] : []),
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

  if (isAdminRoute) {
    return (
      <header className="border-b border-teal-100/60">
        <div className="flex w-full items-center justify-between gap-4 px-6 py-5 sm:px-10 lg:px-16">
          <a href="/" className="font-display text-xl text-teal-700">
            AfyaCheck
          </a>

          <div className="flex items-center gap-4">
            <LanguageSwitcher />
            <button
              type="button"
              onClick={handleSignOut}
              className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-soft hover:text-ink"
            >
              <LogOut aria-hidden="true" size={16} />
              {t('nav.signOut')}
            </button>
          </div>
        </div>

        {authError && (
          <div className="border-t border-teal-100/60 px-6 py-3 sm:px-10 lg:px-16">
            <StatusMessage tone="error">{authError}</StatusMessage>
          </div>
        )}
      </header>
    )
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

        <div className="hidden items-center gap-4 justify-self-end md:flex">
          <LanguageSwitcher />
          {isAuthenticated ? (
            <button
              type="button"
              onClick={handleSignOut}
              className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-soft hover:text-ink"
            >
              <LogOut aria-hidden="true" size={16} />
              {t('nav.signOut')}
            </button>
          ) : (
            <button
              type="button"
              onClick={handleSignIn}
              className="text-sm font-medium text-ink-soft hover:text-ink"
            >
              {t('nav.signIn')}
            </button>
          )}
        </div>

        <button
          type="button"
          onClick={() => setOpen((o) => !o)}
          aria-expanded={open}
          aria-label={open ? t('nav.closeMenu') : t('nav.openMenu')}
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
            <LanguageSwitcher />
            {isAuthenticated ? (
              <button
                type="button"
                onClick={handleSignOut}
                className="inline-flex items-center gap-1.5 text-left text-sm font-medium text-ink-soft hover:text-ink"
              >
                <LogOut aria-hidden="true" size={16} />
                {t('nav.signOut')}
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
                {t('nav.signIn')}
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
