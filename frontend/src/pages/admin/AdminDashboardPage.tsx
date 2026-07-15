import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Chart as ChartJS, ArcElement, CategoryScale, LinearScale, BarElement, Tooltip, Legend } from 'chart.js'
import { Bar, Doughnut } from 'react-chartjs-2'
import { apiClient } from '../../api/client'

ChartJS.register(ArcElement, CategoryScale, LinearScale, BarElement, Tooltip, Legend)

interface ChartData {
  labels: string[]
  data: number[]
  backgroundColors?: string[]
}

interface DashboardData {
  stats: {
    totalUsers: number
    activeUsers: number
    totalQuestionnaires: number
    newUsersThisMonth: number
    totalQuestions: number
  }
  riskDistribution: ChartData
  questionTypeDistribution: ChartData
  serviceHealth: { mlServiceHealthy: boolean; decisionTreeHealthy: boolean }
}

export function AdminDashboardPage() {
  const [data, setData] = useState<DashboardData | null>(null)

  useEffect(() => {
    apiClient.get<DashboardData>('/admin/dashboard').then((response) => setData(response.data))
  }, [])

  if (!data) {
    return <div className="page-loading text-center py-5">Loading...</div>
  }

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="h3">Admin Dashboard</h1>
        <div className="d-flex gap-2">
          <Link to="/admin/users" className="btn btn-outline-success">
            Manage Users
          </Link>
          <Link to="/admin/questions" className="btn btn-outline-success">
            Manage Questions
          </Link>
          <a href="/api/admin/export/csv" className="btn btn-success">
            Export CSV
          </a>
        </div>
      </div>

      <div className="row g-3 mb-4">
        <StatCard label="Total Users" value={data.stats.totalUsers} />
        <StatCard label="Active Users" value={data.stats.activeUsers} />
        <StatCard label="New This Month" value={data.stats.newUsersThisMonth} />
        <StatCard label="Total Questions" value={data.stats.totalQuestions} />
      </div>

      <div className="row g-3 mb-4">
        <div className="col-md-6">
          <div className="card">
            <div className="card-body">
              <h5 className="card-title">Risk Level Distribution</h5>
              <Doughnut
                data={{
                  labels: data.riskDistribution.labels,
                  datasets: [{ data: data.riskDistribution.data, backgroundColor: data.riskDistribution.backgroundColors }],
                }}
              />
            </div>
          </div>
        </div>
        <div className="col-md-6">
          <div className="card">
            <div className="card-body">
              <h5 className="card-title">Question Types</h5>
              <Bar
                data={{
                  labels: data.questionTypeDistribution.labels,
                  datasets: [{ label: 'Questions', data: data.questionTypeDistribution.data, backgroundColor: '#198754' }],
                }}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <h5 className="card-title">Service Health</h5>
          <div className="d-flex gap-4">
            <HealthBadge label="ML Service" healthy={data.serviceHealth.mlServiceHealthy} />
            <HealthBadge label="Decision Tree Service" healthy={data.serviceHealth.decisionTreeHealthy} />
          </div>
        </div>
      </div>
    </div>
  )
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="col-md-3">
      <div className="card text-center">
        <div className="card-body">
          <div className="h3 mb-0">{value}</div>
          <div className="text-muted small">{label}</div>
        </div>
      </div>
    </div>
  )
}

function HealthBadge({ label, healthy }: { label: string; healthy: boolean }) {
  return (
    <div>
      <span className={`badge ${healthy ? 'bg-success' : 'bg-danger'}`}>
        {label}: {healthy ? 'Healthy' : 'Degraded (fallback mode)'}
      </span>
    </div>
  )
}
