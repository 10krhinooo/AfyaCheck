import type { QuestionnaireEndResponse } from '../../lib/types'

// The questionnaire's final /api/questionnaire/next response already contains the full
// risk assessment, so we hand it off via sessionStorage instead of re-fetching immediately.
// ResultsPage falls back to GET /api/results/latest (Phase 3) when this is empty, e.g. on
// a direct link or page refresh.
const KEY_PREFIX = 'afyacheck:result:'
const LAST_SESSION_KEY = 'afyacheck:last-session'

export function storeQuestionnaireResult(result: QuestionnaireEndResponse) {
  sessionStorage.setItem(KEY_PREFIX + result.sessionId, JSON.stringify(result))
  sessionStorage.setItem(LAST_SESSION_KEY, result.sessionId)
}

// ResultsPage's sessionId normally arrives via router state, which a refresh loses,
// this lets a reloaded /app/results still find the assessment from this browser session.
export function readLastSessionId(): string | null {
  return sessionStorage.getItem(LAST_SESSION_KEY)
}

export function readStoredResult(sessionId: string): QuestionnaireEndResponse | null {
  const raw = sessionStorage.getItem(KEY_PREFIX + sessionId)
  return raw ? (JSON.parse(raw) as QuestionnaireEndResponse) : null
}

export function clearStoredResult(sessionId: string) {
  sessionStorage.removeItem(KEY_PREFIX + sessionId)
}
