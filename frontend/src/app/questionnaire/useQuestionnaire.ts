import { useCallback, useEffect, useState } from 'react'
import { apiPost } from '../../lib/api-client'
import { isEndOfSurvey, type Question, type QuestionnaireEndResponse, type QuestionnaireResponse } from '../../lib/types'

interface State {
  sessionId: string | null
  question: Question | null
  canGoBack: boolean
  loading: boolean
  submitting: boolean
  error: string | null
  result: QuestionnaireEndResponse | null
}

// User-facing copy for each failure point, always paired with a concrete next step
// (offered via the caller's retry/goBack action) rather than the raw fetch error text.
const START_ERROR = 'We couldn’t load your assessment. Check your connection and try again.'
const SUBMIT_ERROR = 'We couldn’t save your answer. Check your connection and try again.'
const BACK_ERROR = 'We couldn’t go back to the previous question. Check your connection and try again.'

export function useQuestionnaire() {
  const [state, setState] = useState<State>({
    sessionId: null,
    question: null,
    canGoBack: false,
    loading: true,
    submitting: false,
    error: null,
    result: null,
  })
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    let cancelled = false
    setState((s) => ({ ...s, loading: true, error: null }))
    apiPost<QuestionnaireResponse>('/api/questionnaire/start', {})
      .then((response) => {
        if (cancelled) return
        if (isEndOfSurvey(response)) {
          setState((s) => ({ ...s, loading: false, result: response, sessionId: response.sessionId }))
        } else {
          setState((s) => ({
            ...s,
            loading: false,
            sessionId: response.sessionId,
            question: response.question,
            canGoBack: response.canGoBack,
          }))
        }
      })
      .catch(() => {
        if (!cancelled) setState((s) => ({ ...s, loading: false, error: START_ERROR }))
      })
    return () => {
      cancelled = true
    }
  }, [attempt])

  const retry = useCallback(() => setAttempt((a) => a + 1), [])

  const submitAnswer = useCallback(
    async (key: string, value: string) => {
      if (!state.sessionId) return
      setState((s) => ({ ...s, submitting: true, error: null }))
      try {
        const response = await apiPost<QuestionnaireResponse>('/api/questionnaire/next', {
          sessionId: state.sessionId,
          answers: { [key]: value },
        })
        if (isEndOfSurvey(response)) {
          setState((s) => ({ ...s, submitting: false, result: response, question: null }))
        } else {
          setState((s) => ({
            ...s,
            submitting: false,
            question: response.question,
            canGoBack: response.canGoBack,
          }))
        }
      } catch {
        setState((s) => ({ ...s, submitting: false, error: SUBMIT_ERROR }))
      }
    },
    [state.sessionId],
  )

  const goBack = useCallback(async () => {
    if (!state.sessionId) return
    setState((s) => ({ ...s, submitting: true, error: null }))
    try {
      const response = await apiPost<QuestionnaireResponse>('/api/questionnaire/back', {
        sessionId: state.sessionId,
      })
      if (!isEndOfSurvey(response)) {
        setState((s) => ({
          ...s,
          submitting: false,
          question: response.question,
          canGoBack: response.canGoBack,
        }))
      }
    } catch {
      setState((s) => ({ ...s, submitting: false, error: BACK_ERROR }))
    }
  }, [state.sessionId])

  return { ...state, submitAnswer, goBack, retry }
}
