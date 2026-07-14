[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/l6edej4a)
[![Open in Visual Studio Code](https://classroom.github.com/assets/open-in-vscode-2e0aaae1b6195c2367325f4f02e2d04e9abb55f0b24a779b69b11b9e10269abc.svg)](https://classroom.github.com/online_ide?assignment_repo_id=19902936&assignment_repo_type=AssignmentRepo)

# AfyaCheck

## About

AfyaCheck is a web-based STI risk assessment tool. A user answers an
adaptive questionnaire, receives a machine-learning-driven risk score
and recommendations, and can locate nearby health centers for
follow-up care. An admin dashboard gives staff visibility into
questionnaire usage and lets them manage the question bank and user
accounts.

## How it works

- **Auth**: email/password registration with email verification,
  password reset, and Google/GitHub OAuth2 login, built on Spring
  Security.
- **Questionnaire**: a multi-step, one-question-per-page adaptive
  form. Each next question is chosen by a decision-tree Python
  microservice based on answers so far.
- **Risk assessment**: once the questionnaire is complete, answers
  are sent to a separate ML Python microservice for a risk score and
  recommendations. If that service is unavailable, the app falls
  back to a rule-based heuristic so users still get a result.
- **Health centers**: a Google Maps/Places-backed page to find
  nearby clinics, hospitals, and pharmacies.
- **Admin dashboard**: usage stats and charts, question bank
  management (add/edit/delete), and user management (enable/disable,
  promote to admin).

## Tech stack

- **Backend**: Spring Boot 3, Spring Security, Spring Data JPA,
  Thymeleaf, Postgres
- **ML/decision services**: two standalone Python services
  (`ml-service/`, `python-service/`) called over HTTP
- **Frontend**: server-rendered Thymeleaf templates with Bootstrap

## Running locally

1. Set the environment variables listed in
   `src/main/resources/application.properties.example` (database
   credentials, mail credentials, OAuth2 client id/secret, Google
   Maps API key, etc.). Never commit real values into
   `application.properties`.
2. Have a Postgres database available matching `DB_URL`.
3. `./gradlew bootRun`
4. Optionally start the Python services in `ml-service/` and
   `python-service/` for real ML predictions and adaptive
   questioning; without them the app runs on its fallback heuristics.

A `dev` Spring profile (`SPRING_PROFILES_ACTIVE=dev`) enables verbose
SQL/security logging for local debugging.
