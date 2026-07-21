import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, LinkButton } from '../../components/Button'
import { Card } from '../../components/Card'
import { ProgressBar } from '../../components/ProgressBar'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { useTranslation } from '../../lib/i18n'
import { storeQuestionnaireResult } from '../results/resultStorage'
import { QuestionForm } from './QuestionForm'
import { useQuestionnaire } from './useQuestionnaire'

export default function QuestionnairePage() {
  const { question, canGoBack, loading, submitting, error, result, submitAnswer, goBack, retry } =
    useQuestionnaire()
  const { t } = useTranslation()
  const headingRef = useRef<HTMLHeadingElement>(null)
  const navigate = useNavigate()

  // WCAG: move focus to the new question's heading on every transition, and announce
  // it via an aria-live region, so screen reader users don't need to manually re-read.
  useEffect(() => {
    if (question) headingRef.current?.focus()
    // eslint-disable-next-line react-hooks/exhaustive-deps -- refocus only on question change, not every render
  }, [question?.key])

  // A consent-denied end has no risk assessment behind it (the survey stopped before any
  // question that feeds the model was asked), so there's nothing for /app/results to load.
  // Show the decline message here instead of navigating to a page that would just error.
  useEffect(() => {
    if (result && !result.consentDenied) {
      storeQuestionnaireResult(result)
      navigate('/app/results', { state: { sessionId: result.sessionId } })
    }
  }, [result, navigate])

  if (loading) {
    return (
      <main className="mx-auto max-w-xl px-6 py-12" aria-busy="true">
        <span className="sr-only">{t('questionnaire.loading')}</span>
        <Skeleton className="h-2 w-full rounded-full" />
        <Skeleton className="mt-8 h-64 w-full p-8" />
      </main>
    )
  }

  // An error with no question loaded means the initial fetch failed: nothing to show
  // behind it, so replace the page. An error that happens once a question is already on
  // screen (answer submit, back navigation) is shown inline instead, so the user doesn't
  // lose their place and can immediately retry from where they were.
  if (error && !question) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center">
        <div className="inline-flex text-left">
          <StatusMessage tone="error">{t(error)}</StatusMessage>
        </div>
        <div>
          <Button variant="secondary" className="mt-6" onClick={retry}>
            {t('questionnaire.tryAgain')}
          </Button>
        </div>
      </main>
    )
  }

  if (result?.consentDenied) {
    return (
      <main className="mx-auto max-w-xl px-6 py-16 text-center">
        <h1 className="text-2xl text-ink">{t('questionnaire.consentDeniedTitle')}</h1>
        <p className="mt-2 text-ink-soft">{result.endMessage}</p>
        <div className="mt-6">
          <LinkButton href="/" variant="primary">
            {t('questionnaire.consentDeniedCta')}
          </LinkButton>
        </div>
      </main>
    )
  }

  if (!question) return null

  return (
    <main className="mx-auto max-w-xl px-6 py-12">
      <div aria-live="polite" className="sr-only">
        {t('questionnaire.questionCounter')
          .replace('{index}', String(question.questionIndex))
          .replace('{total}', String(question.totalQuestions))}
        : {question.text}
      </div>

      <ProgressBar
        value={question.questionIndex}
        max={question.totalQuestions}
        label={question.sectionTitle ?? t('questionnaire.progressLabel')}
      />

      <Card key={question.key} className="mt-8 animate-fade-in-up p-8 motion-reduce:animate-none">
        <h1
          ref={headingRef}
          tabIndex={-1}
          className="text-2xl text-ink outline-none"
        >
          {question.text}
        </h1>
        {question.description && <p className="mt-2 text-ink-soft">{question.description}</p>}

        {error && (
          <div className="mt-4">
            <StatusMessage tone="error">{t(error)}</StatusMessage>
          </div>
        )}

        <QuestionForm
          question={question}
          submitting={submitting}
          onSubmit={(value) => submitAnswer(question.key, value)}
        />
      </Card>

      {canGoBack && (
        <Button variant="ghost" className="mt-4" onClick={goBack} disabled={submitting}>
          &larr; {t('questionnaire.back')}
        </Button>
      )}
    </main>
  )
}
