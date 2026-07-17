import type { RouteRecord } from 'vite-react-ssg'
import Layout from './Layout'
import Landing from './pages/landing/Landing'
import CallbackPage from './lib/auth/CallbackPage'
import About from './pages/about/About'
import Faq from './pages/faq/Faq'
import PrivacyPolicy from './pages/legal/PrivacyPolicy'
import TermsOfService from './pages/legal/TermsOfService'
import NotFound from './pages/not-found/NotFound'

// "/" plus the static marketing/legal pages are prerendered to static HTML at build time
// (see vite.config.ts ssgOptions.includedRoutes) since they carry real SEO value, unlike the
// auth-walled/session-personalized /app/** SPA routes below.
//
// NavBar/Footer are mounted once in Layout.tsx, so every route here shares identical chrome —
// no per-route shell wrapper needed (AppShell was removed once the header stopped being
// /app/**-specific).
//
// Every /app/** route is route.lazy-loaded so each page ships its own chunk instead of all
// sharing one "app" bundle (see budget.json's per-route budgets).
export const routes: RouteRecord[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Landing /> },
      { path: 'about', element: <About /> },
      { path: 'faq', element: <Faq /> },
      { path: 'privacy', element: <PrivacyPolicy /> },
      { path: 'terms', element: <TermsOfService /> },
      // Silent OIDC redirect, no visual UI worth the shared chrome, so NavBar/Footer hide
      // themselves on this path (see NavBar.tsx / Footer.tsx).
      { path: 'app/callback', element: <CallbackPage /> },
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
      { path: 'app/admin/admins', lazy: () => import('./app/admin/AdminAdminsPage.lazy') },
      { path: 'app/admin/questions', lazy: () => import('./app/admin/AdminQuestionsPage.lazy') },
      { path: 'app/admin/audit-log', lazy: () => import('./app/admin/AdminAuditLogPage.lazy') },
      { path: '*', element: <NotFound /> },
    ],
  },
]
