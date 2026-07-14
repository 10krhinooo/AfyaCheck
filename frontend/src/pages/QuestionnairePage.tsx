import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiClient } from '../api/client'

interface QuestionData {
  key: string
  text: string
  description?: string
  type: string
  sectionTitle?: string
  options?: string[]
  questionIndex?: number
  totalQuestions?: number
  progress?: number
  validation?: { min?: number; max?: number }
}

interface QuestionStepResponse {
  sessionId: string
  question: QuestionData
  answers: Record<string, string>
  canGoBack: boolean
  ended: false
}

interface ResultStepResponse {
  sessionId: string
  ended: true
  consentDenied: boolean
  endMessage?: string
  riskScore?: number
  riskLevel?: string
  recommendations?: string[]
  confidence?: number
  modelUsed?: string
}

type StepResponse = QuestionStepResponse | ResultStepResponse

export function QuestionnairePage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<StepResponse | null>(null)
  const [selectedValue, setSelectedValue] = useState<string>('')
  const [selectedOptions, setSelectedOptions] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiClient
      .post<StepResponse>('/questions/start')
      .then((response) => setStep(response.data))
      .catch(() => setError('Failed to start the assessment. Please try again.'))
      .finally(() => setIsLoading(false))
  }, [])

  if (isLoading) {
    return <div className="page-loading text-center py-5">Loading...</div>
  }

  if (error) {
    return (
      <div className="container py-5 text-center">
        <div className="alert alert-danger">{error}</div>
      </div>
    )
  }

  if (!step) {
    return null
  }

  if (step.ended) {
    return <ResultSummary result={step} onDone={() => navigate(`/results/${step.sessionId}`)} />
  }

  const question = step.question

  async function submitAnswer() {
    if (!step || step.ended) return
    setIsLoading(true)
    setError(null)

    const answerValue = question.type === 'multiple_choice' ? selectedOptions : selectedValue

    try {
      const response = await apiClient.post<StepResponse>('/questions/next', {
        sessionId: step.sessionId,
        answers: { [question.key]: answerValue },
      })
      setStep(response.data)
      setSelectedValue('')
      setSelectedOptions([])
    } catch {
      setError('Failed to submit your answer. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  async function goBack() {
    if (!step || step.ended) return
    setIsLoading(true)
    setError(null)
    try {
      const response = await apiClient.post<StepResponse>('/questions/back', {
        sessionId: step.sessionId,
      })
      setStep(response.data)
      setSelectedValue('')
      setSelectedOptions([])
    } catch {
      setError('Failed to go back. Please try again.')
    } finally {
      setIsLoading(false)
    }
  }

  function toggleOption(option: string) {
    setSelectedOptions((prev) =>
      prev.includes(option) ? prev.filter((o) => o !== option) : [...prev, option],
    )
  }

  const canSubmit =
    question.type === 'multiple_choice' ? selectedOptions.length > 0 : selectedValue.trim() !== ''

  return (
    <div className="container py-5" style={{ maxWidth: 640 }}>
      {question.progress !== undefined && (
        <div className="progress mb-4" style={{ height: 8 }}>
          <div
            className="progress-bar bg-success"
            style={{ width: `${question.progress}%` }}
            role="progressbar"
            aria-valuenow={question.progress}
            aria-valuemin={0}
            aria-valuemax={100}
          />
        </div>
      )}

      {question.sectionTitle && <p className="text-muted small text-uppercase mb-1">{question.sectionTitle}</p>}
      <h2 className="h4 mb-2">{question.text}</h2>
      {question.description && <p className="text-muted">{question.description}</p>}

      <div className="my-4">
        {renderInput(question, selectedValue, setSelectedValue, selectedOptions, toggleOption)}
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      <div className="d-flex justify-content-between">
        <button className="btn btn-outline-secondary" onClick={goBack} disabled={!step.canGoBack || isLoading}>
          Back
        </button>
        <button className="btn btn-success" onClick={submitAnswer} disabled={!canSubmit || isLoading}>
          {isLoading ? 'Processing...' : 'Next'}
        </button>
      </div>
    </div>
  )
}

function renderInput(
  question: QuestionData,
  selectedValue: string,
  setSelectedValue: (value: string) => void,
  selectedOptions: string[],
  toggleOption: (option: string) => void,
) {
  switch (question.type) {
    case 'yes_no':
      return (
        <div className="d-flex gap-3">
          {['Yes', 'No'].map((option) => (
            <button
              key={option}
              type="button"
              className={`btn ${selectedValue === option ? 'btn-success' : 'btn-outline-success'} flex-grow-1`}
              onClick={() => setSelectedValue(option)}
            >
              {option}
            </button>
          ))}
        </div>
      )
    case 'multiple_choice':
      return (
        <div className="d-flex flex-column gap-2">
          {(question.options ?? []).map((option) => (
            <div className="form-check" key={option}>
              <input
                className="form-check-input"
                type="checkbox"
                id={`opt-${option}`}
                checked={selectedOptions.includes(option)}
                onChange={() => toggleOption(option)}
              />
              <label className="form-check-label" htmlFor={`opt-${option}`}>
                {option}
              </label>
            </div>
          ))}
        </div>
      )
    case 'choice':
      return (
        <div className="d-flex flex-column gap-2">
          {(question.options ?? []).map((option) => (
            <button
              key={option}
              type="button"
              className={`btn text-start ${selectedValue === option ? 'btn-success' : 'btn-outline-success'}`}
              onClick={() => setSelectedValue(option)}
            >
              {option}
            </button>
          ))}
        </div>
      )
    case 'number':
      return (
        <input
          type="number"
          className="form-control"
          value={selectedValue}
          min={question.validation?.min}
          max={question.validation?.max}
          onChange={(e) => setSelectedValue(e.target.value)}
        />
      )
    default:
      return (
        <input
          type="text"
          className="form-control"
          value={selectedValue}
          onChange={(e) => setSelectedValue(e.target.value)}
        />
      )
  }
}

function ResultSummary({ result, onDone }: { result: ResultStepResponse; onDone: () => void }) {
  if (result.consentDenied) {
    return (
      <div className="container py-5 text-center" style={{ maxWidth: 600 }}>
        <h1 className="h3 mb-4">Assessment Ended</h1>
        <div className="alert alert-info">{result.endMessage}</div>
        <p className="text-muted">
          We respect your decision. If you change your mind, you can always start a new assessment.
        </p>
      </div>
    )
  }

  return (
    <div className="container py-5 text-center" style={{ maxWidth: 600 }}>
      <h1 className="h3 mb-4">Assessment Complete</h1>
      <p className="text-muted mb-4">{result.endMessage}</p>
      <button className="btn btn-success btn-lg" onClick={onDone}>
        View Your Results
      </button>
    </div>
  )
}
