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

function isAdminUser(user: User | null): boolean {
  if (!user) return false
  const realmAccess = user.profile.realm_access as { roles?: string[] } | undefined
  return realmAccess?.roles?.includes('ADMIN') ?? false
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
