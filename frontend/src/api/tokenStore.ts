// Holds the current access token in memory only. Never persisted to
// localStorage or sessionStorage, so it disappears on tab close/reload,
// at which point a fresh one is minted from the httpOnly refresh cookie.
let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}
