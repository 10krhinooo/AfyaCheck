import type { QuestionnaireEndResponse } from '../../lib/types'

// The questionnaire's final /api/questionnaire/next response already contains the full
// risk assessment, so we hand it off via sessionStorage instead of re-fetching immediately.
// ResultsPage falls back to GET /api/results/latest (Phase 3) when this is empty, e.g. on
// a direct link or page refresh.
const KEY_PREFIX = 'afyacheck:result:'

export function storeQuestionnaireResult(result: QuestionnaireEndResponse) {
  sessionStorage.setItem(KEY_PREFIX + result.sessionId, JSON.stringify(result))
}

export function readStoredResult(sessionId: string): QuestionnaireEndResponse | null {
  const raw = sessionStorage.getItem(KEY_PREFIX + sessionId)
  return raw ? (JSON.parse(raw) as QuestionnaireEndResponse) : null
}
