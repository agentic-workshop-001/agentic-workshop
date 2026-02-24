# Runbook – Naturgy Workshop UI

## Prerequisites

| Tool | Minimum |
|------|---------|
| Java | 21 |
| Maven | 3.9+ |
| Node.js | 18+ |
| npm | 9+ |

> **UI Standards**: The frontend follows Naturgy React Standards (MUI v5).
> See `_data/specs/react-standards.md` for the full spec.

## Start Backend

```bash
cd backend
mvn spring-boot:run
# Starts on http://localhost:8080
# H2 in-memory DB is seeded from sample CSVs on every boot.
```

Seed data paths (inside the JAR / src/main/resources):
- `db/samples/meters.csv`
- `db/samples/contracts.csv`
- `db/samples/readings.csv`

Source CSVs (SSOT):
- `_data/db/samples/meters.csv`
- `_data/db/samples/contracts.csv`
- `_data/db/samples/readings.csv`

## Start Frontend

```bash
cd frontend
c# first time only
npm run dev
# Starts on http://localhost:5173
# All /api calls are proxied to http://localhost:8080
```

## Demo Steps (UI)

1. **Meters** (`/meters`)
   - View the two seeded meters (MTR0001, MTR0002).
   - Click **+ New Meter** → fill in fields → Save.
   - Edit or Delete any meter.
   - Optional: upload `_data/db/samples/meters.csv` via the CSV Import panel.

2. **Contracts** (`/contracts`)
   - View the two seeded contracts (CONT001 FIXED, CONT002 FLAT).
   - Filter by Meter ID.
   - Click **+ New Contract** → choose **Contract Type**:
     - `FIXED` → only the *Fixed Price/kWh* field is shown.
     - `FLAT`  → *Monthly Fee*, *Included kWh*, *Overage Price/kWh* are shown.
   - Optional: upload `_data/db/samples/contracts.csv`.

3. **Readings** (`/readings`)
   - Filter by Meter ID and/or date range, then click **Apply**.
   - Click **+ New Reading** → pick meter, date, hour (0-23), kWh, quality.
   - Delete a reading (no PUT per spec; re-POST after delete to update).
   - Optional: upload `_data/db/samples/readings.csv`.

4. **Billing / Invoices** (`/billing`)
   - Enter a period (e.g. `2026-01`) in the *Run Billing* panel and click **▶ Generate Invoices**.
   - The result shows how many invoices were generated (idempotent — re-running same period is a no-op).
   - The invoice table appears below; filter by period if needed.
   - Click **⬇ PDF** to download the invoice PDF.

## Optional curl commands

```bash
# Run billing for January 2026
curl -X POST "http://localhost:8080/api/billing/run?period=2026-01"

# List invoices for that period
curl "http://localhost:8080/api/invoices?period=2026-01"

# Download PDF (replace INV-xxx with actual invoice ID)
curl -o invoice.pdf "http://localhost:8080/api/invoices/INV-xxx/pdf"

# Import meters CSV
curl -F "file=@_data/db/samples/meters.csv" http://localhost:8080/api/meters/import
```

## SSOT References

- Field definitions: `_data/specs/csv-spec.txt`
- Billing logic:     `_data/specs/logic-spec.txt`
- Design decisions:  `_data/specs/clarifications.txt`

## Running Tests

```bash
# Backend unit + integration tests
mvn -f backend/pom.xml test

# Frontend production build check
npm --prefix frontend run build
```
