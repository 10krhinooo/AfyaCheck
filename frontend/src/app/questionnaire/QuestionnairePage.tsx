import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { ProgressBar } from '../../components/ProgressBar'
import { storeQuestionnaireResult } from '../results/resultStorage'
import { QuestionForm } from './QuestionForm'
import { useQuestionnaire } from './useQuestionnaire'

export default function QuestionnairePage() {
  const { question, canGoBack, loading, submitting, error, result, submitAnswer, goBack } = useQuestionnaire()
  const headingRef = useRef<HTMLHeadingElement>(null)
  const navigate = useNavigate()

  // WCAG: move focus to the new question's heading on every transition, and announce
  // it via an aria-live region, so screen reader users don't need to manually re-read.
  useEffect(() => {
    if (question) headingRef.current?.focus()
    // eslint-disable-next-line react-hooks/exhaustive-deps -- refocus only on question change, not every render
  }, [question?.key])

  useEffect(() => {
    if (result) {
      storeQuestionnaireResult(result)
      navigate('/app/results', { state: { sessionId: result.sessionId } })
    }
  }, [result, navigate])

  if (loading) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center text-ink-soft" aria-busy="true">
        Loading your assessment…
      </main>
    )
  }

  if (error) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center">
        <p className="text-coral-700">{error}</p>
        <Button variant="secondary" className="mt-6" onClick={() => window.location.reload()}>
          Try again
        </Button>
      </main>
    )
  }

  if (!question) return null

  return (
    <main className="mx-auto max-w-xl px-6 py-12">
      <div aria-live="polite" className="sr-only">
        Question {question.questionIndex} of {question.totalQuestions}: {question.text}
      </div>

      <ProgressBar
        value={question.questionIndex}
        max={question.totalQuestions}
        label={question.sectionTitle ?? 'Assessment progress'}
      />

      <Card className="mt-8 p-8">
        <h1
          ref={headingRef}
          tabIndex={-1}
          className="text-2xl text-ink outline-none"
        >
          {question.text}
        </h1>
        {question.description && <p className="mt-2 text-ink-soft">{question.description}</p>}

        <QuestionForm
          question={question}
          submitting={submitting}
          onSubmit={(value) => submitAnswer(question.key, value)}
        />
      </Card>

      {canGoBack && (
        <Button variant="ghost" className="mt-4" onClick={goBack} disabled={submitting}>
          &larr; Back
        </Button>
      )}
    </main>
  )
}
