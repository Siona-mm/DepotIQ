# Upload validation verification — 2026-08-30

Implemented complete catalog CSV validation, reference-only sales imports, depot product creation, and additive stock receipts with duplicate protection.

## Verification

- Backend: 71 tests passed, zero failures/errors (Maven, Java 17).
- Frontend: 12 CSV validation tests passed (`node --test src/utils/csvImport.test.js`).
- Frontend production build passed. Vite reports the existing large application-bundle warning.
- ESLint passed for the CSV helper, its tests, and API module. Changed JSX files passed undefined-variable checks; the existing JSX-unaware unused-variable rule was disabled for that check only.
- A separate PostgreSQL 16 container and backend on port 18081 applied all 16 Flyway migrations and passed 15 API/database scenarios. These covered complete product creation, blank values, additive stock, unchanged reservations, receipt replay, changed receipt quantities, transaction rollback after an earlier write, unknown IDs, concurrent different and identical receipts, overflow, preservation of catalog details on failure, and valid/invalid sales imports.
- Browser: inspected upload modes, guidance, sample links, and the full-page layout. Uploaded a synthetic invalid refill CSV and confirmed a row-specific required-value error.
- Safety review blocked the browser's successful stock submission test. It was not retried or bypassed; successful stock receipt behavior had already passed the isolated API/database tests.

No existing application database or deployed containers were changed. Temporary test services were stopped after verification. Rebuild backend/frontend once to apply the code and migration; see `docs/external_data_contract.md` for schemas and the command.
