export interface RiskAssessmentDto {
  riskScore: number
  riskLevel: 'Low' | 'Medium' | 'High'
  recommendations: string[]
  createdAt: string
}

export interface LatestResultResponse {
  sessionId: string
  assessment: RiskAssessmentDto
}

export interface HistoryResponse {
  sessionId: string
  assessments: RiskAssessmentDto[]
}
