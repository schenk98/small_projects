# Glosář a business pravidla

## Role

| Role | Oprávnění |
|------|-----------|
| STUDENT | Session, knihy, obchod, vlastní historie RO |
| TEACHER | Plný CRUD kromě odebrání vlastní role TEACHER |

Nový učitel: registrace jako student → jiný učitel přiřadí TEACHER.

## Třída (SchoolClass)

- Filtr pro učitelské přehledy only.
- Student funguje plně i bez třídy.
- M:N ClassMembership; členství lze měnit.

## Peněženka (balance)

Neukládat v DB. Vždy:

`balance = Σ session.points + Σ quiz.points + Σ adjustment.amount − Σ purchase.price`

Session ukládá `points_awarded` v okamžiku uložení (sazby z DB v tu chvíli). Edit/smazání session u učitele mění balance automaticky.

## Výchozí sazby (editovatelné učitelem v DB)

| Klíč | Výchozí |
|------|---------|
| points_per_page | 0 |
| points_per_minute | 0.1 |
| quiz_show_probability | 0.6 |

Žádná systémová anti-cheat omezení (stropy, minuty/stránku).

## Kniha (Book)

- Vlastník = jeden student; žádný sdílený katalog.
- Status: READING | FINISHED.
- Více READING najednou; dropdown při session jen READING.
- Dočtení: session s `marked_finished` → FINISHED, kniha mimo dropdown.

## Reading session

Pole: book, date (default dnes), minutes, pages_from/to (from předvyplnit z book.last_page), note, points_awarded, marked_finished.

Student **nesmí** editovat vlastní historii.

## Kvíz

Po uložení session: náhodně dle `quiz_show_probability`.
Otázka z DB + volný text; **bez správných odpovědí**.
Body = `QuizQuestion.default_points` (nebo override u odpovědi) — ihned v historii; učitel může upravit.

## Obchod

- ShopItem: name, description, price, cooldown_days (nullable).
- Nákup atomický: kontrola balance + cooldown (per student per item, posledních X dní z Purchase).
- Výsledek: FULFILLED záznam nebo odmítnutí (nedostatek bodů / cooldown). Jeden stav — bez pending.
- Problémy: učitel LedgerAdjustment.

## LedgerAdjustment

amount (+/−), reason (krátký text), student_id, teacher_id, created_at.

## Featured

Text only nebo text + obrázek (bytea) jako klikací odkaz (target URL).

## Obrázky

ImageBlob: mime + compressed bytea. MVP: avatar z presetů; featured upload u učitele.

## Audit

AuditEvent: retence default 3 měsíce — hodnota `audit_retention_days` v AppSetting.
Účty, session, nákupy, knihy: držet neomezeně.

## Jazyk UI

MVP: čeština only.
