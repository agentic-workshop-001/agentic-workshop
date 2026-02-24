# Copilot Instructions — Naturgy Workshop (SSOT + no-questions)

## SSOT (Single Source of Truth)
Use ONLY:
- `_data/specs/csv-spec.txt`
- `_data/specs/logic-spec.txt`

Seed data to load into DB:
- `_data/db/samples/meters.csv`
- `_data/db/samples/contracts.csv`
- `_data/db/samples/readings.csv`

## No duplication
Do not copy/restate spec content in docs/comments/agent files.
Only reference paths and implement accordingly.

## No questions / no confirmations
Agents must not ask the user for confirmations or permissions.
If a detail is ambiguous, pick the simplest deterministic default and record it in:
- `_data/specs/clarifications.txt`
(keep it minimal)

## Target outcome
- Backend Spring Boot (Java 21, Maven, H2, JPA):
  - schema/entities from csv-spec
  - validations + billing from logic-spec
  - seed import on startup from sample CSVs
  - CRUD endpoints for 3 tables
  - billing run + invoice list + PDF download
- Frontend React/Vite:
  - CRUD maintenance for 3 tables
  - run billing by period
  - list invoices + download PDF

## Conventions
- API base path `/api`
- Controllers thin, services hold logic
- Money uses BigDecimal + rounding per logic-spec
- Errors: consistent JSON with 400/404 (409 optional)

## Quality
- Unit tests for billing and validations per logic-spec
- At least 1 integration test: seed → billing → invoices → pdf
- Must pass:
  - `mvn -f backend/pom.xml test`
  - `npm --prefix frontend run build`