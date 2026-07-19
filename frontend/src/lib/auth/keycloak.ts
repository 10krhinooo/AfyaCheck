import { UserManager, WebStorageStateStore } from 'oidc-client-ts'

// Authorization Code + PKCE against the "afyacheck" realm (see keycloak/realm-export.json).
// oidc-client-ts generates the PKCE code_verifier/code_challenge automatically for the
// "afyacheck-spa" public client (no client secret — see docker-compose.yml for local dev).
const issuer = import.meta.env.VITE_KEYCLOAK_ISSUER ?? 'http://localhost:8180/realms/afyacheck'
const clientId = import.meta.env.VITE_KEYCLOAK_CLIENT_ID ?? 'afyacheck-spa'

// Lazily constructed: this module is imported from Layout, which also wraps the landing page
// prerendered at build time by vite-react-ssg (a Node/SSR environment with no `window`).
// Constructing UserManager eagerly at module scope would crash that build.
let instance: UserManager | null = null

export function getUserManager(): UserManager {
  if (!instance) {
    instance = new UserManager({
      authority: issuer,
      client_id: clientId,
      redirect_uri: `${window.location.origin}/app/callback`,
      // Must exactly match an entry in the Keycloak client's post.logout.redirect.uris
      // (see keycloak/realm-export.json) -- Keycloak does exact string matching here, and
      // that list is registered with a trailing slash.
      post_logout_redirect_uri: `${window.location.origin}/`,
      response_type: 'code',
      scope: 'openid profile email',
      userStore: new WebStorageStateStore({ store: window.sessionStorage }),
      automaticSilentRenew: true,
    })
  }
  return instance
}

// signinRedirect()/signoutRedirect() both fetch Keycloak's OIDC discovery document
// (issuer/.well-known/...) before navigating the browser away, so a down/unreachable Keycloak
// rejects the promise instead of leaving the user on a browser-level connection-refused page —
// callers should catch this.
export class AuthServiceUnavailableError extends Error {
  constructor() {
    super('The authentication service is unavailable right now. Please try again shortly.')
    this.name = 'AuthServiceUnavailableError'
  }
}

export async function login(returnTo?: string) {
  try {
    await getUserManager().signinRedirect({ state: { returnTo: returnTo ?? '/app/dashboard' } })
  } catch {
    throw new AuthServiceUnavailableError()
  }
}

export async function logout() {
  try {
    await getUserManager().signoutRedirect()
  } catch {
    throw new AuthServiceUnavailableError()
  }
}

export async function getAccessToken(): Promise<string | null> {
  const user = await getUserManager().getUser()
  return user && !user.expired ? user.access_token : null
}
