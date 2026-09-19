# Produktová vize — Čtenářský pas

## Cíl

Webová aplikace, kde žáci evidují čtení a sbírají body do peněženky; body utrácejí v obchodě za výhody uplatnitelné na hodině českého jazyka. Učitel spravuje obsah, ceník, třídy (filtr) a zpětně kontroluje nebo upravuje záznamy.

## Uživatelé

- **Student** — primárně mobil; zadávání session, obchod, vlastní historie (read-only).
- **Teacher** — stejný dashboard s rozšířenými akcemi; CRUD dat; primárně PC.

Role TEACHER = administrace (bez separátní admin role v MVP).

## Klíčové obrazovky

1. Přihlášení / registrace (e-mail + heslo).
2. Dashboard — featured, peněženka, avatar, rozcestník.
3. Nový záznam čtení (+ volitelný kvíz popup).
4. Historie (student: jen svá; učitel: libovolný student, editace).
5. Obchod (student: nákup; učitel: CRUD položek + sazby bodování).

## Mimo scope MVP

Multi-škol / tenant, PWA, i18n, upload avatarů, povinná verifikace e-mailu — viz `OPEN-QUESTIONS.md` (1.0).

## Deploy

MVP: localhost + Docker Compose. Produkce a doména později.
