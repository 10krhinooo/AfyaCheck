import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  // Served from /app/** rather than the static root: the legacy Thymeleaf
  // pages still serve their own assets from static/css and static/js, and
  // building into the shared root would wipe those out from under them
  // (emptyOutDir does exactly that if the two share a directory).
  base: '/app/',
  build: {
    // Ships inside the same Spring Boot jar as a static SPA. See the
    // frontend-build Gradle task wiring this into `./gradlew build`.
    outDir: '../src/main/resources/static/app',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    proxy: {
      // Forwarded to Spring Boot on :8080 during local dev so the browser
      // sees everything as same-origin (no CORS needed for this path).
      '/api': 'http://localhost:8080',
      '/oauth2': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
      '/logout': 'http://localhost:8080',
    },
  },
})
