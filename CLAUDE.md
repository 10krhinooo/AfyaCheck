# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

AfyaCheck is an STI/HIV risk assessment system for the Kenyan market. Users answer an adaptive
questionnaire, get an ML-based risk assessment, and can find nearby health centers. An admin
dashboard lets staff manage users, questions, and usage stats.

The system is three independently-run services:

| Service | Path | Stack | Port | Purpose |
|---|---|---|---|---|
| Backend | `src/` | Spring Boot 3 (Java 21), PostgreSQL, Flyway | 8080 | Auth, questionnaire flow, sessions, admin dashboard, orchestrates the two Python services |
| Decision tree service | `python-service/` | FastAPI, scikit-learn | 8001 | Sequences the next question based on prior answers |
| ML risk prediction service | `ml-service/` | FastAPI, scikit-learn/XGBoost | 8000 | Produces the final HIV/STI risk prediction |

A React SPA (`frontend/`) is being incrementally strangler-fig migrated in to replace the legacy
Thymeleaf UI — see Architecture below.

## Commands

### Backend (Spring Boot, run from repo root)

```bash
./gradlew bootRun              # start the backend on :8080
./gradlew build                # compile + test + Jacoco coverage verification (see Coverage below)
./gradlew test                 # run all tests
./gradlew test --tests "com.kimanga.afyacheck.service.DecisionServiceTest"                              # single class
./gradlew test --tests "com.kimanga.afyacheck.service.DecisionServiceTest.someTestMethod"                # single method
```

Required env vars with **no default** — the app fails to start (not silently runs blank) without
them: `DB_PASSWORD`, `MAIL_PASSWORD`, `REMEMBER_ME_KEY`. Everything else in
`application.properties` has a sane local default (`DB_URL` defaults to port `5432`; this
environment's local Postgres runs on `5433`, so override `DB_URL` accordingly). See
`src/main/resources/application.properties.example` for the full list, and `.env` in repo root
for actual local values (git-ignored). Spring Boot does not read `.env` automatically — export it
first, e.g. `set -a && source .env && set +a`, before `./gradlew bootRun`.

The two Python services are **not** auto-started by the backend (`ml.service.auto.start` /
`decision.tree.service.auto.start` both default `false` in `application.properties`) — start them
manually before hitting endpoints that depend on them.

### Python services

```bash
cd python-service && pip install -r requirements.txt && python decision_tree_service.py   # :8001
cd ml-service && pip install -r requirements.txt && python app.py                          # :8000
```

Both scripts call `uvicorn.run(..., reload=True)` when run as `__main__`, which requires an
import-string app reference and will crash on the reload check — run them via the uvicorn CLI
instead: `uvicorn app:app --host 0.0.0.0 --port 8000` / `uvicorn decision_tree_service:app --host
0.0.0.0 --port 8001`.

No pytest suite exists for either service yet — CI only does a `python -m py_compile` sanity
check on the entrypoint.

### Frontend (React + Vite, run from `frontend/`)

```bash
npm run dev           # vite-react-ssg dev server, proxies /api to http://localhost:8080
npm run build          # tsc -b && vite-react-ssg build -> frontend/dist
npm run lint            # oxlint
npm run typecheck      # tsc -b --noEmit
npm run test            # vitest run
npm run test:e2e        # playwright test
npm run budget:check    # bundle size budget check (scripts/check-bundle-budget.mjs, budget.json)
```

`frontend/dist` is **not yet wired into the backend build** — CI builds and uploads it as a
separate artifact, but nothing copies it into `src/main/resources/static`. `WebConfig` forwards
`/app/**` to `forward:/index.html`, which currently resolves to nothing unless you manually place
a built `index.html`/bundle under `src/main/resources/static`.

## Architecture

### Strangler-fig frontend migration

The backend serves two UIs simultaneously during migration:
- Legacy server-rendered Thymeleaf templates (`src/main/resources/templates/`) for
  `/admin/**`, `/dashboard`, `/login`, `/questionnaire`, etc. — the original UI, being phased out.
- A React SPA under `/app/**`, forwarded by `WebConfig.addViewControllers()` to a single
  `index.html` shell so React Router can resolve client-side routes/deep-links. The same bundle
  prerenders `/` at build time via `vite-react-ssg`; everything else under `/app/**` is
  client-rendered only (auth-walled, no SEO value — see `ssgOptions.includedRoutes` in
  `frontend/vite.config.ts`).
- `SecurityConfig` already points post-login/OAuth2 redirects at the new `/app/dashboard` (React
  route), not the legacy Thymeleaf `/dashboard`.
- `WebContentInterceptor` in `WebConfig` disables caching only on `/admin/**` ("Legacy Thymeleaf
  MVC admin routes only; removed once Phase 6 decommissions them") — the rest of the legacy UI is
  already gone or on its way out ahead of the admin section.

### Backend package layout (`src/main/java/com/kimanga/afyacheck/`)

- `config/` — Spring wiring: `SecurityConfig` (form login + conditionally-wired OAuth2 — the
  `ClientRegistrationRepository` bean is `Optional<>` and OAuth2 login is only registered if
  present, so the app boots fine with OAuth2 unconfigured), `WebConfig` (routing described above),
  `PythonConfig` (validates Python executable/scripts/model files exist at startup, exposes
  service-availability status; does not itself spawn processes), `GoogleMapsConfig`,
  `AdminInitializer` (seeds the admin user), `OAuth2UserInfo`/`OAuth2UserInfoFactory`
  (provider-specific OAuth2 attribute mapping), `GlobalExceptionHandler`.
- `controllers/` — `AuthController`, `DashboardController`, `HealthCenterController`,
  `QuestionController`, `ResultsController`; `controllers/admin/AdminController`.
- `service/` — business logic. `DecisionService` orchestrates the question flow, calling
  `DecisionTreeClient` and `MLService`. `PythonServiceManager` is the actual process-lifecycle
  manager for the two Python services (spawns/monitors/auto-restarts them via `ProcessBuilder`
  when auto-start is enabled — separate from `PythonConfig`, which only validates config).
- `model/` — JPA entities (`User`, `Session`, `Question`, `Questionnaire`, `Answer`,
  `RiskAssessment`, `HealthCenter`, etc.).
- `repository/` — Spring Data JPA repos, roughly one per entity.
- `DTO/` — request/response shapes. Five classes (`HealthCenterResponse`, `LocationRequest`,
  `QuestionProgress`, `SessionStartResponse`, `SessionState`) are explicitly excluded from Jacoco
  coverage in `build.gradle` as dead code.
- `mail/` — `EmailSenderService`, `EmailService` for verification/reset emails.

### Talking to the Python services

`DecisionTreeClient` and `MLService` are both plain `RestTemplate` clients (not `WebClient`,
despite `spring-boot-starter-webflux` being on the classpath). `MLService` holds the canonical
option lists (`MARITAL_STATUS_OPTIONS`, `EDUCATION_OPTIONS`, `WEALTH_INDEX_OPTIONS`,
`CONDOM_USE_OPTIONS`) used to normalize free-form input into the values the Python model expects —
actual encoding/inference happens on the Python side. Note: `MLService` reads its target URL from
`ml.risk.service.url`, a different property key than `ml.service.url` used elsewhere
(`PythonConfig`/`application.properties`) — check which one is actually in effect if the ML
integration misbehaves.

### Database

Flyway migrations live in `src/main/resources/db/migration/` (currently `V1__baseline_schema.sql`,
`V2__seed_sample_data.sql`). `spring.jpa.hibernate.ddl-auto=validate` — schema changes go through
new Flyway migrations, not Hibernate auto-DDL.

### Coverage gate

`build.gradle` wires `jacocoTestCoverageVerification` (minimum **90% line coverage**) into the
`check` task, so `./gradlew build` fails if coverage drops below that threshold.
