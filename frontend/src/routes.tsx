import type { RouteRecord } from 'vite-react-ssg'
import Layout from './Layout'
import AppShell from './app/AppShell'
import Landing from './pages/landing/Landing'
import CallbackPage from './lib/auth/CallbackPage'

// Only "/" is prerendered to static HTML at build time (see vite.config.ts ssgOptions.includedRoutes).
// Everything under /app/** is a normal client-rendered SPA route (auth-walled, no SEO value).
// Questionnaire/results/health-centers stay anonymous-accessible by design (see
// SecurityConfig's permitAll list), only dashboard/admin require Keycloak sign-in.
//
// Every /app/** route is route.lazy-loaded so each page ships its own chunk instead of all
// sharing one "app" bundle (see budget.json's per-route budgets).
export const routes: RouteRecord[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Landing /> },
      // Silent OIDC redirect, no visual UI worth the shared /app/** chrome, so it stays
      // outside AppShell like Landing.
      { path: 'app/callback', element: <CallbackPage /> },
      {
        element: <AppShell />,
        children: [
          {
            path: 'app/questionnaire',
            lazy: async () => ({ Component: (await import('./app/questionnaire/QuestionnairePage')).default }),
          },
          {
            path: 'app/results',
            lazy: async () => ({ Component: (await import('./app/results/ResultsPage')).default }),
          },
          {
            path: 'app/health-centers',
            lazy: async () => ({ Component: (await import('./app/health-centers/HealthCentersPage')).default }),
          },
          // Chart.js is desktop/broadband-only weight (admin console, per the migration plan's
          // bundle budget) — lazy-loaded so public/mobile users never pay for it. Auth is
          // enforced inside each *.lazy.tsx entry via RequireAuth (route.lazy can't combine with
          // route.element, so the guard has to live in the lazily-loaded module itself). Dashboard
          // follows the same *.lazy.tsx pattern for consistency, even though its guard is simpler.
          { path: 'app/dashboard', lazy: () => import('./app/dashboard/DashboardPage.lazy') },
          { path: 'app/admin', lazy: () => import('./app/admin/AdminDashboardPage.lazy') },
          { path: 'app/admin/users', lazy: () => import('./app/admin/AdminUsersPage.lazy') },
          { path: 'app/admin/questions', lazy: () => import('./app/admin/AdminQuestionsPage.lazy') },
        ],
      },
    ],
  },
]
