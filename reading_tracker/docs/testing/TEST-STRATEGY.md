# Test strategie

## Automatické (priorita: student)

- **Unit:** výpočet balance, body session ze sazeb, cooldown nákupu.
- **Integrační:** Spring Boot Test + Testcontainers PostgreSQL — registrace, session → balance, nákup úspěch/neúspěch, dočtení knihy, kvíz probabilisticky (mock/fix probability na 1.0 v testu).

## Manuální

- [`MANUAL-TEACHER-CHECKLIST.md`](MANUAL-TEACHER-CHECKLIST.md) — učitelský CRUD a editace cizí historie.

## Mimo MVP

- Playwright/Cypress smoke pro celý web (1.0).
