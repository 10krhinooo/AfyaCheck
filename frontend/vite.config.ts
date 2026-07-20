/// <reference types="vitest/config" />
import type { UserConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { VitePWA } from 'vite-plugin-pwa'
import type { ViteReactSSGOptions } from 'vite-react-ssg'
import type { InlineConfig as VitestInlineConfig } from 'vitest/node'

// https://vite.dev/config/
export default {
  plugins: [
    react(),
    tailwindcss(),
    // Caches the app shell (JS/CSS/fonts) so a repeat visit loads instantly even on a flaky
    // connection, and lets an in-progress questionnaire keep rendering through brief
    // connectivity drops -- not full offline completion, since answers still have to reach
    // the backend, but resilient to the kind of short network blips common on mobile data
    // in the areas this app targets.
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg'],
      manifest: {
        name: 'AfyaCheck',
        short_name: 'AfyaCheck',
        description: 'Confidential HIV/STI risk assessment',
        theme_color: '#863bff',
        background_color: '#faf7f2',
        display: 'standalone',
        start_url: '/',
        scope: '/',
        icons: [
          { src: '/pwa-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/pwa-512.png', sizes: '512x512', type: 'image/png' },
          { src: '/pwa-maskable-512.png', sizes: '512x512', type: 'image/png', purpose: 'maskable' },
        ],
      },
      workbox: {
        // Precache the app shell only -- API responses (questionnaire/results) are
        // per-session and shouldn't be served stale across different users/sessions on a
        // shared device, so those stay network-only rather than cached.
        globPatterns: ['**/*.{js,css,html,woff,woff2}'],
        navigateFallbackDenylist: [/^\/api\//],
      },
    }),
  ],
  server: {
    proxy: {
      // Forwards to the local Spring Boot backend during dev so cookies/CORS
      // behave the same as the same-origin production deployment (SPA bundled
      // into the Spring Boot jar's static resources under /app/**).
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./tests/setup.ts'],
    // e2e/ holds Playwright specs (a separate runner, see test:e2e) — vitest's default
    // *.spec.ts include pattern would otherwise try to collect them too.
    exclude: ['e2e/**', 'node_modules/**'],
  },
  ssgOptions: {
    // "/" plus the static marketing/legal pages are prerendered to static HTML at build
    // time; everything under /app/** is auth-walled/session-personalized with zero SEO
    // value and stays a normal client-rendered SPA route.
    includedRoutes: () => [
      '/',
      '/about',
      '/faq',
      '/learn',
      '/learn/sti-basics',
      '/learn/hiv-prevention',
      '/learn/testing',
      '/privacy',
      '/terms',
    ],
  } satisfies Partial<ViteReactSSGOptions>,
} satisfies UserConfig & {
  test?: VitestInlineConfig
  ssgOptions?: Partial<ViteReactSSGOptions>
}
