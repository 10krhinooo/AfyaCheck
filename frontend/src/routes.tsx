import type { RouteRecord } from 'vite-react-ssg'
import Layout from './Layout'
import Landing from './pages/landing/Landing'
import QuestionnairePage from './app/questionnaire/QuestionnairePage'
import ResultsPage from './app/results/ResultsPage'
import HealthCentersPage from './app/health-centers/HealthCentersPage'
import DashboardPage from './app/dashboard/DashboardPage'
import CallbackPage from './lib/auth/CallbackPage'
import { RequireAuth } from './lib/auth/RequireAuth'

// Only "/" is prerendered to static HTML at build time (see vite.config.ts ssgOptions.includedRoutes).
// Everything under /app/** is a normal client-rendered SPA route (auth-walled, no SEO value).
// Questionnaire/results/health-centers stay anonymous-accessible by design (see
// SecurityConfig's permitAll list) — only dashboard/admin require Keycloak sign-in.
export const routes: RouteRecord[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Landing /> },
      { path: 'app/callback', element: <CallbackPage /> },
      { path: 'app/questionnaire', element: <QuestionnairePage /> },
      { path: 'app/results', element: <ResultsPage /> },
      { path: 'app/health-centers', element: <HealthCentersPage /> },
      {
        path: 'app/dashboard',
        element: (
          <RequireAuth>
            <DashboardPage />
          </RequireAuth>
        ),
      },
      // Chart.js is desktop/broadband-only weight (admin console, per the migration plan's
      // bundle budget) — lazy-loaded so public/mobile users never pay for it. Auth is
      // enforced inside each *.lazy.tsx entry via RequireAuth (route.lazy can't combine with
      // route.element, so the guard has to live in the lazily-loaded module itself).
      { path: 'app/admin', lazy: () => import('./app/admin/AdminDashboardPage.lazy') },
      { path: 'app/admin/users', lazy: () => import('./app/admin/AdminUsersPage.lazy') },
      { path: 'app/admin/questions', lazy: () => import('./app/admin/AdminQuestionsPage.lazy') },
    ],
  },
]
