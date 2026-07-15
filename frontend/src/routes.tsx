import type { RouteRecord } from 'vite-react-ssg'
import Layout from './Layout'
import Landing from './pages/landing/Landing'
import QuestionnairePage from './app/questionnaire/QuestionnairePage'
import ResultsPage from './app/results/ResultsPage'
import HealthCentersPage from './app/health-centers/HealthCentersPage'
import DashboardPage from './app/dashboard/DashboardPage'
import AdminDashboardPage from './app/admin/AdminDashboardPage'

// Only "/" is prerendered to static HTML at build time (see vite.config.ts ssgOptions.includedRoutes).
// Everything under /app/** is a normal client-rendered SPA route (auth-walled, no SEO value).
export const routes: RouteRecord[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Landing /> },
      { path: 'app/questionnaire', element: <QuestionnairePage /> },
      { path: 'app/results', element: <ResultsPage /> },
      { path: 'app/health-centers', element: <HealthCentersPage /> },
      { path: 'app/dashboard', element: <DashboardPage /> },
      { path: 'app/admin', element: <AdminDashboardPage /> },
    ],
  },
]
