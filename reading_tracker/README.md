# Čtenářský pas (`reading_tracker`)

Interaktivní web pro sledování čtení (2. stupeň ZŠ, český jazyk): body za čtení, obchod výhod, učitelská správa.

**Stack (MVP):** Spring Boot, Thymeleaf, HTMX, PostgreSQL, Flyway, Docker Compose (localhost).

## Spuštění lokálně

Jedním příkazem spustíte celý stack (backend + PostgreSQL):

```powershell
./start-local.ps1
```

Skript automaticky najde volné hostitelské porty; pokud je 8080 zabrané, použije další volný port (např. 8081). Aplikace je pak dostupná na:
- http://localhost:8080 nebo http://localhost:8081
- PostgreSQL na localhost:5432

Pro zastavení:

```powershell
docker compose down
```

Dokumentace pro vývoj a AI agenty: [`docs/AGENT-ORCHESTRATOR.md`](docs/AGENT-ORCHESTRATOR.md).
