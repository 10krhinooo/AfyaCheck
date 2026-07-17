import { ClipboardCheck, HelpCircle, UserPlus, Users, UserCheck } from 'lucide-react'
import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { ChartWithTable } from './ChartWithTable'
import { StatTile } from './StatTile'
import { Button } from '../../components/Button'
import { Card } from '../../components/Card'
import { Skeleton } from '../../components/Skeleton'
import { StatusMessage } from '../../components/StatusMessage'
import { apiFetch } from '../../lib/api-client'
import type { AdminDashboardResponse } from './types'

const LOAD_ERROR = 'We couldn’t load the dashboard data. Check your connection and try again.'

export default function AdminDashboardPage() {
  const [data, setData] = useState<AdminDashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [attempt, setAttempt] = useState(0)

  useEffect(() => {
    setError(null)
    apiFetch<AdminDashboardResponse>('/api/admin/dashboard')
      .then(setData)
      .catch(() => setError(LOAD_ERROR))
  }, [attempt])

  return (
    <main className="w-full px-6 py-10">
      <h1 className="font-display text-2xl text-ink">Admin dashboard</h1>
      <AdminNav />

      {error && (
        <div className="mt-4">
          <StatusMessage
            tone="error"
            action={
              <Button variant="secondary" onClick={() => setAttempt((a) => a + 1)}>
                Try again
              </Button>
            }
          >
            {error}
          </StatusMessage>
        </div>
      )}
      {!data && !error && (
        <div aria-busy="true">
          <span className="sr-only">Loading…</span>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-24" />
            ))}
          </div>
          <div className="mt-8 grid gap-6 lg:grid-cols-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <Skeleton key={i} className="h-72" />
            ))}
          </div>
        </div>
      )}

      {data && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            <StatTile label="Total users" value={data.totalUsers} icon={<Users size={16} />} />
            <StatTile label="Active users" value={data.activeUsers} icon={<UserCheck size={16} />} />
            <StatTile
              label="Assessments completed"
              value={data.totalQuestionnaires}
              icon={<ClipboardCheck size={16} />}
            />
            <StatTile label="New this month" value={data.newUsersThisMonth} icon={<UserPlus size={16} />} />
            <StatTile label="Questions in bank" value={data.totalQuestions} icon={<HelpCircle size={16} />} />
          </div>

          <div className="mt-8 grid gap-6 lg:grid-cols-2">
            <ChartWithTable title="User growth (last 6 months)" data={data.userGrowthData} variant="bar" />
            <ChartWithTable title="Answer completions (last 30 days)" data={data.answerCompletionsData} variant="bar" />
            <ChartWithTable title="Question type distribution" data={data.questionTypeDistribution} variant="doughnut" />
            <ChartWithTable title="Section distribution" data={data.sectionDistribution} variant="doughnut" />
          </div>

          <section className="mt-8">
            <h2 className="text-lg text-ink">Recent users</h2>

            <div className="mt-3 hidden overflow-x-auto sm:block">
              <table className="w-full text-left text-sm">
                <thead>
                  <tr className="border-b border-teal-100 text-ink-soft">
                    <th scope="col" className="py-2">
                      Name
                    </th>
                    <th scope="col" className="py-2">
                      Email
                    </th>
                    <th scope="col" className="py-2">
                      Joined
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentUsers.map((user, i) => (
                    <tr
                      key={user.id}
                      className={`border-b border-teal-50 transition-colors hover:bg-teal-50/60 ${
                        i % 2 === 1 ? 'bg-paper-dim/40' : ''
                      }`}
                    >
                      <td className="py-2 text-ink">{user.name}</td>
                      <td className="py-2 text-ink-soft">{user.email}</td>
                      <td className="py-2 text-ink-soft">{new Date(user.joinDate).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            <div className="mt-3 space-y-3 sm:hidden">
              {data.recentUsers.map((user) => (
                <Card key={user.id} className="p-4">
                  <p className="font-medium text-ink">{user.name}</p>
                  <p className="mt-1 text-sm text-ink-soft">{user.email}</p>
                  <p className="mt-1 text-sm text-ink-soft">
                    Joined {new Date(user.joinDate).toLocaleDateString()}
                  </p>
                </Card>
              ))}
            </div>
          </section>
        </>
      )}
    </main>
  )
}
