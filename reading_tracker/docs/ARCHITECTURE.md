# Architektura

## Přehled

Monolit **Spring Boot 3** + **Thymeleaf** + **HTMX** + **Spring Security** + **Spring Data JPA** + **Flyway** + **PostgreSQL**.

```text
Browser (mobile/desktop)
    → Spring MVC Controllers
    → Services (ledger, shop, session, quiz)
    → Repositories
    → PostgreSQL (včetně bytea pro obrázky)
```

## HTMX

- Modal kvízu po uložení session (fragment).
- Učitel: toggle edit režim na obchodě / sazbách bez full SPA.

## Bezpečnost

- Form login; role-based access (STUDENT vs TEACHER).
- Student endpoints: jen vlastní data.
- Teacher: globální CRUD s výjimkou self-demotion TEACHER.

## Balance service

Centralizovaný výpočet balance(studentId) z agregací — použít v dashboardu, obchodě před nákupem.

## Docker (MVP)

`docker-compose.yml` (cíl MVP-01): app + postgres, profil dev localhost.

## Produkce (poznámka)

Jedna instance pro jednu školu/třídu učitelů; ~300 uživatelů zvládne malý VPS/EC2 + Postgres v Compose. Detail deploy až po MVP.
