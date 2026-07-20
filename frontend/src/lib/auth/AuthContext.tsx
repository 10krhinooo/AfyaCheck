import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { User } from 'oidc-client-ts'
import { getUserManager } from './keycloak'

interface AuthState {
  user: User | null
  isLoading: boolean
  isAuthenticated: boolean
  isAdmin: boolean
}

const AuthContext = createContext<AuthState>({
  user: null,
  isLoading: true,
  isAuthenticated: false,
  isAdmin: false,
})

// Keycloak's realm-roles protocol mapper only puts realm_access on the access token by
// default (not the ID token, i.e. not user.profile), same claim the backend reads off the
// bearer token in SecurityConfig/DashboardController, so decode it the same way here rather
// than trusting the ID token to carry a claim it never will unless the realm config changes.
function isAdminUser(user: User | null): boolean {
  if (!user?.access_token) return false
  try {
    const payload = user.access_token.split('.')[1]
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))) as {
      realm_access?: { roles?: string[] }
    }
    return decoded.realm_access?.roles?.includes('ADMIN') ?? false
  } catch {
    return false
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  useEffect(() => {
    // Only runs client-side (useEffect never fires during vite-react-ssg's build-time
    // prerender of "/"), so it's safe for getUserManager() to touch window here.
    const userManager = getUserManager()
    userManager
      .getUser()
      .then((u) => setUser(u && !u.expired ? u : null))
      .catch(() => setUser(null))
      .finally(() => setIsLoading(false))

    const onLoaded = (u: User) => setUser(u)
    const onUnloaded = () => setUser(null)
    userManager.events.addUserLoaded(onLoaded)
    userManager.events.addUserUnloaded(onUnloaded)
    return () => {
      userManager.events.removeUserLoaded(onLoaded)
      userManager.events.removeUserUnloaded(onUnloaded)
    }
  }, [])

  return (
    <AuthContext.Provider
      value={{ user, isLoading, isAuthenticated: user !== null, isAdmin: isAdminUser(user) }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  return useContext(AuthContext)
}
