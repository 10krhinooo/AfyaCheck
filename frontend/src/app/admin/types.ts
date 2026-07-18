export interface ChartData {
  labels: string[]
  data: number[]
  backgroundColors?: string[]
}

export interface AdminDashboardResponse {
  totalUsers: number
  activeUsers: number
  totalQuestionnaires: number
  newUsersThisMonth: number
  totalQuestions: number
  userGrowthData: ChartData
  answerCompletionsData: ChartData
  questionTypeDistribution: ChartData
  sectionDistribution: ChartData
  recentUsers: AdminUser[]
}

export interface AdminUser {
  id: string
  name: string
  email: string
  joinDate: string
  lastActive: string
  enabled: boolean
  questionnaireCount: number
  role: string
}

export interface UsersResponse {
  users: AdminUser[]
  totalUsers: number
  adminUsersCount: number
}

export interface AdminQuestion {
  id: number
  questionKey: string
  questionText: string
  description?: string
  questionType: string
  options?: string[]
  minValue?: number
  maxValue?: number
  sectionTitle?: string
  displayOrder?: number
  isActive: boolean
}

export interface QuestionsResponse {
  questions: AdminQuestion[]
  answerStats: Record<string, unknown>
  questionTypes: string[]
}

export interface AuditLogEntry {
  id: number
  actorEmail: string
  action: string
  targetType?: string
  targetId?: string
  details?: string
  createdAt: string
}

export interface AuditLogResponse {
  entries: AuditLogEntry[]
}
