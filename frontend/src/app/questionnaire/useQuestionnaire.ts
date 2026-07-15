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

  useEffect(() => {
    let cancelled = false
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
      .catch((err: Error) => {
        if (!cancelled) setState((s) => ({ ...s, loading: false, error: err.message }))
      })
    return () => {
      cancelled = true
    }
  }, [])

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
      } catch (err) {
        setState((s) => ({ ...s, submitting: false, error: (err as Error).message }))
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
    } catch (err) {
      setState((s) => ({ ...s, submitting: false, error: (err as Error).message }))
    }
  }, [state.sessionId])

  return { ...state, submitAnswer, goBack }
}
