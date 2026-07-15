/// <reference types="vitest/config" />
import type { UserConfig } from 'vite'
import react from '@vitejs/plugin-react'
import type { ViteReactSSGOptions } from 'vite-react-ssg'
import type { InlineConfig as VitestInlineConfig } from 'vitest/node'

// https://vite.dev/config/
export default {
  plugins: [react()],
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
  },
  ssgOptions: {
    // Only "/" (the landing page) is prerendered to static HTML at build time;
    // everything under /app/** is auth-walled/session-personalized with zero
    // SEO value and stays a normal client-rendered SPA route.
    includedRoutes: () => ['/'],
  } satisfies Partial<ViteReactSSGOptions>,
} satisfies UserConfig & {
  test?: VitestInlineConfig
  ssgOptions?: Partial<ViteReactSSGOptions>
}
