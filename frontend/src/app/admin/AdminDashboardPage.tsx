import { useEffect, useState } from 'react'
import { AdminNav } from './AdminNav'
import { ChartWithTable } from './ChartWithTable'
import { StatTile } from './StatTile'
import { apiFetch } from '../../lib/api-client'
import type { AdminDashboardResponse } from './types'

export default function AdminDashboardPage() {
  const [data, setData] = useState<AdminDashboardResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    apiFetch<AdminDashboardResponse>('/api/admin/dashboard')
      .then(setData)
      .catch((err: Error) => setError(err.message))
  }, [])

  return (
    <main className="mx-auto max-w-5xl px-6 py-10">
      <h1 className="text-2xl text-ink">Admin dashboard</h1>
      <AdminNav />

      {error && <p className="text-coral-700">{error}</p>}
      {!data && !error && <p className="text-ink-soft">Loading…</p>}

      {data && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            <StatTile label="Total users" value={data.totalUsers} />
            <StatTile label="Active users" value={data.activeUsers} />
            <StatTile label="Assessments completed" value={data.totalQuestionnaires} />
            <StatTile label="New this month" value={data.newUsersThisMonth} />
            <StatTile label="Questions in bank" value={data.totalQuestions} />
          </div>

          <div className="mt-8 grid gap-6 lg:grid-cols-2">
            <ChartWithTable title="User growth (last 6 months)" data={data.userGrowthData} variant="bar" />
            <ChartWithTable title="Answer completions (last 30 days)" data={data.answerCompletionsData} variant="bar" />
            <ChartWithTable title="Question type distribution" data={data.questionTypeDistribution} variant="doughnut" />
            <ChartWithTable title="Section distribution" data={data.sectionDistribution} variant="doughnut" />
          </div>

          <section className="mt-8">
            <h2 className="text-lg text-ink">Recent users</h2>
            <div className="mt-3 overflow-x-auto">
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
                  {data.recentUsers.map((user) => (
                    <tr key={user.id} className="border-b border-teal-50">
                      <td className="py-2 text-ink">{user.name}</td>
                      <td className="py-2 text-ink-soft">{user.email}</td>
                      <td className="py-2 text-ink-soft">{new Date(user.joinDate).toLocaleDateString()}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </>
      )}
    </main>
  )
}
