[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/l6edej4a)
[![Open in Visual Studio Code](https://classroom.github.com/assets/open-in-vscode-2e0aaae1b6195c2367325f4f02e2d04e9abb55f0b24a779b69b11b9e10269abc.svg)](https://classroom.github.com/online_ide?assignment_repo_id=19902936&assignment_repo_type=AssignmentRepo)

# AfyaCheck

AfyaCheck is an STI/HIV risk assessment system. Users answer a guided questionnaire, get a machine learning based risk assessment, and can find nearby health centers for follow up care. An admin dashboard lets staff manage users, review questions, and monitor activity.

## Features

- Authentication: registration, login, email verification, password reset, remember me, optional Google/GitHub OAuth2
- Adaptive questionnaire that sequences follow up questions using a decision tree model
- Risk assessment powered by a machine learning model, with results tied to the user's session
- Health center finder backed by the Google Maps API
- Admin dashboard for managing users, questions, and viewing usage statistics

## Architecture

The system is made up of three services:

| Service | Path | Stack | Purpose |
|---|---|---|---|
| Backend | `src/` | Spring Boot 3 (Java 21), PostgreSQL, Flyway | Auth, questionnaire flow, sessions, admin dashboard, orchestrates the two ML services |
| Decision tree service | `python-service/` | FastAPI, scikit-learn | Sequences the next question based on prior answers |
| ML risk prediction service | `ml-service/` | FastAPI, scikit-learn/XGBoost | Produces the final HIV/STI risk prediction from questionnaire answers |

## Prerequisites

- Java 21
- PostgreSQL
- Python 3.13 (for the two FastAPI services)
- A Gmail (or other SMTP) account for outbound email

## Setup

### 1. Database

Create a PostgreSQL database named `AfyaCheck`. Schema migrations run automatically via Flyway on startup.

### 2. Backend configuration

Copy `src/main/resources/application.properties.example` to a location outside version control (or export the same names as real environment variables) and fill in real values:

```bash
cp src/main/resources/application.properties.example .env.local
```

Required variables:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` — PostgreSQL connection
- `MAIL_USERNAME`, `MAIL_PASSWORD` — SMTP credentials used for verification and password reset emails
- `REMEMBER_ME_KEY` — a long random string used to sign remember me cookies
- `GOOGLE_MAPS_API_KEY` — used by the health center finder
- `APP_BASE_URL` — the base URL used to build links in emails and OAuth2 redirects

`GOOGLE_OAUTH_CLIENT_ID`/`SECRET` and `GITHUB_OAUTH_CLIENT_ID`/`SECRET` are optional and only needed if you enable OAuth2 login.

`DB_PASSWORD`, `MAIL_PASSWORD`, and `REMEMBER_ME_KEY` have no default: the app fails to start if they are unset rather than running with a blank credential.

### 3. Python services

```bash
cd python-service
pip install -r requirements.txt
python decision_tree_service.py

cd ../ml-service
pip install -r requirements.txt
python app.py
```

The backend expects both services to be reachable; see `PythonConfig` for the configured ports.

### 4. Run the backend

```bash
./gradlew bootRun
```

The app starts on `http://localhost:8080`.

## Running tests

```bash
./gradlew test
```

Jacoco coverage reports are generated under `build/reports/jacoco`.

## Continuous integration

GitHub Actions runs the backend test suite and coverage report on every push and pull request. See `.github/workflows/ci.yml`.
