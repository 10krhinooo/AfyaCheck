import { NavLink } from 'react-router-dom'

const links = [
  { to: '/app/admin', label: 'Dashboard', end: true },
  { to: '/app/admin/users', label: 'Users' },
  { to: '/app/admin/questions', label: 'Questions' },
  { to: '/app/admin/audit-log', label: 'Audit log' },
]

export function AdminNav() {
  return (
    <nav className="mb-8 flex gap-1 border-b border-teal-100" aria-label="Admin sections">
      {links.map((link) => (
        <NavLink
          key={link.to}
          to={link.to}
          end={link.end}
          className={({ isActive }) =>
            `px-4 py-3 text-sm font-medium transition-colors ${
              isActive ? 'border-b-2 border-teal-600 text-teal-700' : 'text-ink-soft hover:text-ink'
            }`
          }
        >
          {link.label}
        </NavLink>
      ))}
    </nav>
  )
}
