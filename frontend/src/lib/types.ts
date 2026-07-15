export type QuestionType = 'radio' | 'number' | 'text'

export interface Question {
  key: string
  text: string
  description?: string
  type: QuestionType
  sectionTitle?: string
  options?: string[]
  min?: number
  max?: number
  progress: number
  questionIndex: number
  totalQuestions: number
}

export interface QuestionnaireStepResponse {
  sessionId: string
  question: Question
  canGoBack: boolean
  end?: false
}

export interface QuestionnaireEndResponse {
  sessionId: string
  end: true
  consentDenied?: boolean
  endMessage?: string
  riskScore?: number
  riskLevel?: 'Low' | 'Medium' | 'High'
  recommendations?: string[]
  confidence?: number
  modelUsed?: string
  questionsAnswered?: number
}

export type QuestionnaireResponse = QuestionnaireStepResponse | QuestionnaireEndResponse

export function isEndOfSurvey(response: QuestionnaireResponse): response is QuestionnaireEndResponse {
  return response.end === true
}
