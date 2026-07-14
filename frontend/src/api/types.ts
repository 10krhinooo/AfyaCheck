export interface CurrentUser {
  id: number
  email: string
  username: string
  name: string
  roles: string[]
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: CurrentUser
}

export interface ApiErrorBody {
  error?: string
  fields?: Record<string, string>
}
