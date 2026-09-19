# Agent orchestrátor — Čtenářský pas

Hlavní vstupní bod pro AI agenty po naplánování dalšího kroku.

## Pořadí fází agentů

1. **Analýza** — pouze pokud slice odkazuje na nevyřešené rozhodnutí v `OPEN-QUESTIONS.md` (MVP: většinou přeskočit).
2. **Implementace** — soubor v `docs/implementation/MVP-XX-*.md`.
3. **Testování** — soubor v `docs/testing/MVP-XX-*.md` + `TEST-STRATEGY.md`.

## Před každým krokem načti

- [`GLOSSARY-AND-RULES.md`](GLOSSARY-AND-RULES.md)
- [`DOMAIN-MODEL.md`](DOMAIN-MODEL.md)
- [`ARCHITECTURE.md`](ARCHITECTURE.md)

## Mapa MVP slice (implementace)

| ID | Název | Soubor (cíl) |
|----|--------|----------------|
| MVP-01 | Projekt skeleton, Flyway, Docker Compose | `implementation/MVP-01-skeleton.md` |
| MVP-02 | Auth, role STUDENT/TEACHER | `implementation/MVP-02-auth.md` |
| MVP-03 | Třídy M:N, filtr učitele | `implementation/MVP-03-classes.md` |
| MVP-04 | Knihy (per student), stavy READING/FINISHED | `implementation/MVP-04-books.md` |
| MVP-05 | Reading session + computed balance | `implementation/MVP-05-sessions.md` |
| MVP-06 | Kvíz 60 %, volný text | `implementation/MVP-06-quiz.md` |
| MVP-07 | Obchod, nákup atomický, cooldown | `implementation/MVP-07-shop.md` |
| MVP-08 | Ledger adjustments (učitel) | `implementation/MVP-08-adjustments.md` |
| MVP-09 | Dashboard, featured, avatar preset | `implementation/MVP-09-dashboard.md` |
| MVP-10 | Učitelský CRUD + edit historie žáka | `implementation/MVP-10-teacher-crud.md` |
| MVP-11 | Audit retence 3 měsíce (config DB) | `implementation/MVP-11-audit.md` |

Slice soubory se doplňují postupně; agent vždy implementuje **jeden** slice na krok, pokud není určeno jinak.

## Pravidla implementace

- MVC: controller → service → repository; business pravidla v glossary.
- Peněženka **nikdy** jako uložený sloupec — vždy výpočet z historie.
- Učitel nesmí sám sobě odebrat roli TEACHER.
- Obrázky MVP: `bytea` v DB, komprese na serveru.
- UI: responzivní; HTMX pro kvíz modal a lehké učitelské editace.

## Po dokončení slice

- Spustit testy dle `testing/MVP-XX-*.md`.
- U učitelských částí projít položky v `testing/MANUAL-TEACHER-CHECKLIST.md` relevantní pro slice.
