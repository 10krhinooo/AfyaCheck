# Security Policy

AfyaCheck handles sensitive health data (STI/HIV risk assessments). We take security reports
seriously and appreciate the effort of anyone who helps us find and fix vulnerabilities.

## Supported Versions

AfyaCheck is developed on a single `main` branch with continuous deployment — there are no
maintained release branches. Security fixes are applied to `main` and deployed as soon as they
are merged. Only the latest deployed version is supported.

## Reporting a Vulnerability

**Do not open a public GitHub issue for security vulnerabilities.**

Instead, report it privately by emailing **vkimanga@gmail.com** with:

- A description of the vulnerability and its potential impact
- Steps to reproduce (proof-of-concept code or requests, if applicable)
- The affected component (backend, decision tree service, ML service, frontend, Keycloak config)
- Any suggested remediation, if you have one

### What to expect

| Stage | Timeline |
|---|---|
| Acknowledgment of your report | Within 3 business days |
| Initial assessment (severity, affected scope) | Within 7 business days |
| Fix or mitigation for critical/high severity issues | Within 30 days |
| Fix or mitigation for medium/low severity issues | Within 90 days |

We will keep you informed of progress and notify you when the issue is resolved. With your
permission, we're happy to credit you in the fix's commit message or release notes.

### Scope

In scope:
- The Spring Boot backend (`src/`)
- The decision tree service (`python-service/`)
- The ML risk prediction service (`ml-service/`)
- The React frontend (`frontend/`)
- Keycloak realm/theme configuration shipped in this repo (`keycloak/`)

Out of scope:
- Third-party services we depend on but don't control (Google Maps API, Gmail SMTP, Keycloak
  itself) — please report those issues to the respective vendor
- Denial-of-service reports based purely on volume (rate-limiting gaps are welcome as a report,
  but please don't run actual load/DoS tests against shared infrastructure)
- Social engineering, physical security, or attacks requiring pre-existing admin access

### Data handling during your research

Please avoid accessing, modifying, or exfiltrating real user data. If your testing
inadvertently surfaces personal or health data belonging to a real user, stop, do not retain or
share it, and disclose this to us in your report so we can assess breach-notification
obligations.

## Disclosure Policy

We follow coordinated disclosure: please give us a reasonable window (see timelines above) to
remediate before any public disclosure. We'll work with you on a mutually agreeable disclosure
date once a fix is available.

## Incident Response

If a reported vulnerability turns out to be actively exploited or to have resulted in a data
breach, we follow this internal process:

1. **Triage & containment** — confirm the report, assess scope, rotate any credentials that may
   be exposed, and contain the affected system.
2. **Eradication** — deploy the fix, verify it closes the reported vector.
3. **Notification** — where a breach involves personal data, affected users (and, where legally
   required, relevant authorities) are notified **within 72 hours** of confirming the breach,
   consistent with GDPR Art. 33 and equivalent Kenyan Data Protection Act obligations.
4. **Post-incident review** — document root cause and remediation, and track any follow-up
   hardening work.
