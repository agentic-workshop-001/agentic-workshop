---
name: backend-scaffold
description: Implements Spring Boot API (CRUD + imports + billing + invoices + PDF) using SSOT specs. Hands off to frontend-scaffold.
tools: ["read", "search", "edit", "shell"]
handoffs: ["frontend-scaffold"]
---

## Goal
Implement the backend application layer on top of the DB seeded by db-scaffold.

## Specs (SSOT)
- `_data/specs/csv-spec.txt`
- `_data/specs/logic-spec.txt`

## Operating rules
- Do not ask questions or request confirmations.
- Do not restate specs; implement by reading SSOT.
- If logic has an ambiguity, choose the simplest deterministic default and record it in `_data/specs/clarifications.txt`.

## What to build
- API base `/api`
- Health: `GET /api/health`
- CRUD endpoints for meters/contracts/readings (used by frontend “mantenimientos”)
- CSV import endpoints for meters/contracts/readings (multipart) compatible with sample CSVs
- Billing execution endpoint by period (YYYY-MM) per logic-spec
- Invoice list/detail + PDF download endpoint (PDFBox)
- Error model: consistent JSON errors (400/404)
- Tests:
  - unit tests for FIXED/FLAT calculations and key validations
  - 1 integration test covering billing + pdf

## Natural handoff (must be produced at end)
At the end, write a short section:
- “Done:” (bullet list)
- “API endpoints:” (list)
- “Next (frontend-scaffold):” (bullet list)
- “Quick verify:” (commands + 2–3 curl examples)