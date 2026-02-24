---
name: db-scaffold
description: Builds JPA/H2 persistence from _data/specs/csv-spec.txt and seeds DB from _data/db/samples/*.csv.
tools:
  - read
  - search
  - edit
  - shell
---

## Goal
Create the database layer and seed import so the backend can start with data immediately.

## Specs (SSOT)
- Schema/constraints: `_data/specs/csv-spec.txt`
- Logic rules awareness only (do not implement billing here): `_data/specs/logic-spec.txt`

## Seed data
- `_data/db/samples/meters.csv`
- `_data/db/samples/contracts.csv`
- `_data/db/samples/readings.csv`

## Operating rules
- Do not ask questions or request confirmations.
- Do not restate specs; implement by reading SSOT.
- If anything is ambiguous for DB modeling, choose the simplest default and record it in `_data/specs/clarifications.txt`.

## What to build
- JPA entities + enums + repositories consistent with csv-spec
- Constraints enforced (PK/FK/unique as per csv-spec)
- Startup seeding that imports the three CSVs in order (meters → contracts → readings)
- Idempotent seed behavior (restart does not duplicate)
- Minimal smoke test for boot + seed

## Natural handoff (must be produced at end)
At the end, write a short section:
- “Done:” (bullet list)
- “Next (backend-scaffold):” (bullet list)
- “Quick verify:” (commands)