# AfyaCheck

AfyaCheck is an STI/HIV risk assessment system. Users answer a guided questionnaire, get a machine learning based risk assessment, and can find nearby health centers for follow up care. An admin dashboard lets staff manage users, review questions, and monitor activity.

A React SPA is being incrementally strangler-fig migrated in to replace the legacy Thymeleaf UI — see Architecture below.

## Features

- Authentication via Keycloak (Authorization Code + PKCE from the React SPA), including a themed hosted login/registration/password-reset flow
- Adaptive questionnaire that sequences follow up questions using a decision tree model
- Risk assessment powered by a machine learning model, with results tied to the user's session
- Health center finder backed by the Google Maps API
- Admin dashboard for managing users, questions, and viewing usage statistics

## Architecture

The system is made up of four services:

| Service | Path | Stack | Purpose |
|---|---|---|---|
| Backend | `src/` | Spring Boot 3 (Java 21), PostgreSQL, Flyway | Questionnaire flow, sessions, admin dashboard, orchestrates the two ML services, validates Keycloak-issued JWTs |
| Decision tree service | `python-service/` | FastAPI, scikit-learn | Sequences the next question based on prior answers |
| ML risk prediction service | `ml-service/` | FastAPI, scikit-learn/XGBoost | Produces the final HIV/STI risk prediction from questionnaire answers |
| Frontend | `frontend/` | React + Vite (`vite-react-ssg`), TypeScript | SPA under `/app/**`, replacing the legacy Thymeleaf templates route by route |

Identity is owned by Keycloak — see `keycloak/realm-export.json` for the realm/client config and `keycloak/themes/afyacheck/` for the custom login theme (extends Keycloak's built-in `keycloak.v2` theme with AfyaCheck's palette and type).

## Prerequisites

- Java 21
- PostgreSQL
- Python 3.13 (for the two FastAPI services)
- Node.js (for the frontend)
- Docker (for local Keycloak)
- A Gmail (or other SMTP) account for outbound email

## Setup

### 1. Database

Create a PostgreSQL database named `AfyaCheck`. Schema migrations run automatically via Flyway on startup.

### 2. Keycloak

```bash
docker compose up -d
```

Starts Keycloak on `http://localhost:8180` (admin console: `admin` / `admin`) and imports the
`afyacheck` realm and `afyacheck-spa` client from `keycloak/realm-export.json`, along with the
custom login theme in `keycloak/themes/afyacheck/`. This is a dev-only setup — a real deployment
runs Keycloak against Postgres with proper secrets, not `start-dev`'s ephemeral storage.

### 3. Backend configuration

Create a `.env` file in the repo root (see `src/main/resources/application.properties.example`
for the full list of names) and fill in real values. It's loaded automatically into Spring's
environment at startup via the `spring-dotenv` dependency — no manual export step needed.

Required variables:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — PostgreSQL connection
- `MAIL_USERNAME`, `MAIL_PASSWORD` — SMTP credentials used for outbound email
- `REMEMBER_ME_KEY` — a long random string used to sign remember me cookies
- `GOOGLE_MAPS_API_KEY` — used by the health center finder
- `APP_BASE_URL` — the base URL used to build links in emails

`DB_PASSWORD`, `MAIL_PASSWORD`, and `REMEMBER_ME_KEY` have no default: the app fails to start if they are unset rather than running with a blank credential.

The two Python services are not auto-started by the backend (`ml.service.auto.start` /
`decision.tree.service.auto.start` both default `false`) — start them manually before hitting
endpoints that depend on them.

### 4. Python services

Both scripts call `uvicorn.run(..., reload=True)` when run directly, which requires an
import-string app reference — run them via the uvicorn CLI instead:

```bash
cd python-service
pip install -r requirements.txt
uvicorn decision_tree_service:app --host 0.0.0.0 --port 8001

cd ../ml-service
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

The backend expects both services to be reachable; see `PythonConfig` for the configured ports.

### 5. Run the backend

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080`.

### 6. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

Starts the Vite dev server, which proxies `/api/**` to `http://localhost:8080` and talks to
Keycloak directly on `:8180`.

## Running tests

```bash
./gradlew test
```

Jacoco coverage reports are generated under `build/reports/jacoco`, with a minimum 90% line
coverage gate enforced on `./gradlew build`.

```bash
cd frontend
npm run lint         # oxlint
npm run typecheck    # tsc -b --noEmit
npm run test         # vitest
npm run test:e2e     # playwright
```

## Continuous integration

GitHub Actions runs the backend test suite and coverage report, and the frontend lint/typecheck/test
suite, on every push and pull request. See `.github/workflows/ci.yml`.
