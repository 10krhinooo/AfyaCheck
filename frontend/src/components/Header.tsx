import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function Header() {
  const { user, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    navigate('/login')
  }

  return (
    <nav className="navbar navbar-expand-lg navbar-dark" style={{ backgroundColor: '#198754' }}>
      <div className="container-fluid">
        <Link className="navbar-brand fw-bold" to="/">
          AfyaCheck
        </Link>
        {user && (
          <div className="d-flex align-items-center gap-3">
            <Link className="nav-link text-white" to="/dashboard">
              Dashboard
            </Link>
            <Link className="nav-link text-white" to="/results/history">
              My Assessments
            </Link>
            <Link className="nav-link text-white" to="/health-centers">
              Health Centers
            </Link>
            {isAdmin && (
              <Link className="nav-link text-white" to="/admin/dashboard">
                Admin
              </Link>
            )}
            <button className="btn btn-outline-light btn-sm" onClick={handleLogout}>
              Logout
            </button>
          </div>
        )}
      </div>
    </nav>
  )
}
