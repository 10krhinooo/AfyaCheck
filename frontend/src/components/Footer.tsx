import { Link, useLocation } from 'react-router-dom'

const links = [
  { to: '/about', label: 'About' },
  { to: '/faq', label: 'FAQ' },
  { to: '/privacy', label: 'Privacy Policy' },
  { to: '/terms', label: 'Terms of Service' },
]

// Shared footer, mounted once in Layout.tsx alongside NavBar so every route (Landing
// included) gets the same chrome instead of Landing rolling its own.
export function Footer() {
  const { pathname } = useLocation()
  if (pathname === '/app/callback') return null

  return (
    <footer className="border-t border-teal-100 py-10">
      <div className="mx-auto flex max-w-7xl flex-col items-center gap-4 px-6 text-center sm:flex-row sm:justify-between sm:text-left">
        <p className="text-sm text-ink-soft">
          Your answers are confidential and used only to generate your assessment.
        </p>
        <nav aria-label="Footer" className="flex flex-wrap items-center justify-center gap-x-6 gap-y-2">
          {links.map((link) => (
            <Link key={link.to} to={link.to} className="text-sm font-medium text-ink-soft hover:text-ink">
              {link.label}
            </Link>
          ))}
        </nav>
      </div>
    </footer>
  )
}
