import { defineConfig } from '@playwright/test'

// Strangler-seam E2E suite (per the migration plan): runs against the fully assembled
// Spring Boot jar + bundled SPA + real Postgres, not this frontend's dev server alone.
export default defineConfig({
  testDir: './e2e',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
  },
})
