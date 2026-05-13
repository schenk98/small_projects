# Poe Pet App — Questions and Answers

## Resolved decisions

- Auth: login required; one account ↔ one email ↔ one pet document.
- Email verification required before login.
- Backend: Maven + Spring Boot; frontend: React + TypeScript + Vite.
- Time simulation: lazy from `pets.lastSimulationAt`.
- Minigame energy: per-game cost from `minigames` collection.
- Rewards / happiness: DB-driven strategies; preview via dashboard + `GET /api/minigames/reward-preview`.
- Shop: consumables (`inventory_items`) and **cosmetics** (`COSMETIC` shop rows → `pets.ownedVisualAssetCodes`).
- Pet visuals: **layered stage** (background + pet mood PNG + optional foreground); **Customize** equips scene + per-mood overrides (starter or owned only).
- Hunger-critical mood is **`playing_dead`** (UI “Playing dead”), not `dead`.
- Pet art: AI-generated PNGs acceptable; use a **reference image per species** for consistency; prefer alpha for layering.
- Local Mongo: Docker Compose; seed **`mongodb/init/01-seed.js`** on **first volume create** only (see `mongodb/README.md`).
- Password policy and JWT refresh: as implemented in backend.

## Open items (non-blocking)

- Exact tuning coefficients for hunger / happiness / energy curves.
  - To be tested - good for now
- Default shop prices and cosmetic prices (currently dev-friendly).
  - To be tested - good for now
- Puzzle swap: optional user uploads vs only curated / stock / AI URLs (`App.tsx` `startPuzzle`).
  - we should generate images of our main pet on some background for this, but temporary images are ok for now
- Connect 4 / other AI depths and balancing.
  - this is already working well - no changes needed for now
- Whether minigames stay modal vs dedicated routes.
  - I feel like having them modal is better for future expansion, changes etc.
- Cloud region and hosting choice for a future deploy.
  - aws east europe or something like that

## Open items (AI Pet track)

These are tracked in more detail in `documentation/SOURCE_OF_TRUTH.md` section **7.4.5**.

- Model choice: which small model do we run (and why)? What are licensing constraints?
  - add it to roadmap as whole step including investigation, take into account our usecase etc.

- Runtime choice: `llama.cpp` / GGUF vs `transformers` / HF vs other.
  - I would prefer ollama, but we might want to wait for our desicion regarding model first - add it to roadmap

- Hosting: CPU-only vs GPU; latency and cost targets; which AWS instance family?
  - cpu only, this is just side project, that will have up to like 3 users at a time and up to 20 registered accounts. In the future we can scale up, but we will go with this usecase

- Security: how does pet backend authenticate to AI service (API key vs mTLS vs signed JWT)?
  - since it will be my app running copy of model, we will just have like token generated or something - we should explore industry standard

- Data policy: do we store chat history? If yes, retention and where?
  - I guess we should store like current session, but it could be in memory - second option is to have it on side of ai app, one session per pet or something with removing older parts of conversation - like remember last 5 messages + personality context or something

- Reliability: fallback behavior when AI service is down; timeouts; rate limiting strategy.
  - when the ai service is down fallback will be like meowing and barking (like several random strings - _barks cheerfully_, _Woof?_, _Makes curious noise_, ...)

## Open items (SQL / deployment / SOAP track)

These are tracked in more detail in `documentation/SOURCE_OF_TRUTH.md` section **7.5.8**.

- SQL choice:
  - decided: use PostgreSQL first

- Activity/history granularity:
  - decided first scope: core pet actions + minigame results + AI chat usage
  - still open: how verbose each event payload should be
  - current direction: the more data the better, with future Elasticsearch-style analysis in mind

- Achievements:
  - decided: permanent achievements only first
  - shared daily challenges are now implemented as 3 generated daily goals on the Progress page
  - AI chat usage should be part of history from the start

- Containerization / AWS:
  - decided: one main Compose-based container setup for the main app if possible
  - AI stays separate and is not part of the main app container stack
  - containerization is now in place for frontend + backend + MongoDB + PostgreSQL + notification SOAP service
  - dev-only MailHog now lives behind the Compose `dev` profile
  - containerization is preparation; **formal roadmap order ends with AWS (EC2) deploy** — see `documentation/ROADMAP.md` “Last milestone: AWS”
  - decided first deploy path: Linux VM / EC2

- SOAP notification side-service:
  - decided first delivery mode: real email
  - decided first notification types:
    - hungry pet notification below a low threshold (current direction: `< 15%`)
    - daily AI summary / daily AI-based notification
  - both first-version notification types should have their own toggle button in settings
  - current implementation status:
    - those two toggles exist in app settings and are stored in PostgreSQL
    - the standalone SOAP notification service exists
    - the main backend now sends low-hunger reminders and daily summaries through that service
    - delivery attempts are recorded in SQL for dedupe/audit
  - still open: which additional notification types should follow after that

# AI kontejner app research:

To, co plánuješ, je v roce 2026 naprosto ideální cesta pro menší projekty. Díky rozmachu tzv. SLMs (Small Language Models) už k rozumné konverzaci nepotřebuješ obří servery s GPU. Pokud ti stačí úroveň „lidská interakce“ a neplánuješ s AI řešit kvantovou fyziku, CPU instance na AWS ti udělají skvělou službu.

Zde je tvůj průzkum pro „Low-cost AI hosting“:

1. Výběr modelů pro rok 2026 (CPU-Friendly)

Pro běh na CPU v reálném čase (aby uživatel nečekal 10 sekund na každé slovo) jsou nejlepší modely do velikosti 4 miliard parametrů v tzv. 4-bitové kvantizaci (GGUF).
Model Velikost Paměť (RAM) Charakteristika
Llama 4 Scout (1B) ~1 mld. < 1 GB Extrémně rychlý. Na CPU odpovídá okamžitě. Ideální pro jednoduché instrukce a chat.
Phi-4 Mini (3.8B) ~4 mld. ~3 GB Král poměru cena/výkon. Od Microsoftu. Kvalitou se vyrovná dřívějším GPT-4 modelům. Skvělý na „lidské“ povídání.
Gemma 4 (2B) ~2 mld. ~2 GB Od Googlu. Velmi kreativní model, dobrý pro přirozenou mluvu, lépe si poradí s češtinou než Phi.

    Můj tip: Pokud chceš, aby to bylo levné a stabilní pro 2-3 lidi najednou, sáhni po Phi-4 Mini. Je to momentálně nejchytřejší model, který se ještě pohodlně vejde do levných AWS instancí.

2. Odhad měsíčních nákladů na AWS (EC2)

Pro tyto modely doporučuji instance řady Graviton (t4g). Jsou to ARM procesory od AWS, které jsou levnější a efektivnější než klasický Intel/AMD.
Typ instance vCPU / RAM Cena/měsíc (On-Demand) Vhodný model
t4g.small 2 vCPU / 2 GB ~$15 – 18 USD Llama 4 Scout (1B)
t4g.medium 2 vCPU / 4 GB ~$30 – 35 USD Phi-4 Mini, Gemma 4 (2B)
c7g.large 2 vCPU / 4 GB ~$45 – 50 USD Rychlejší odezva (Compute optimized)

Ceny jsou orientační pro region us-east-1 (N. Virginia), v Evropě mohou být o 10-15 % vyšší. 3. Jak ušetřit ještě víc (Klíčové triky)

    Spot Instances: Pokud ti nevadí, že by se instance mohla výjimečně restartovat (což u chatu pro 20 lidí přežiješ), použij „Spot“. Cena klesne na ~$5 – 8 USD měsíčně. To je pro PoC bezkonkurenční.

    Kvantizace (GGUF): Musíš použít model ve formátu GGUF. Ten je „ořezaný“ tak, aby neběhal v grafické paměti, ale v normální RAM.

    Vypínání (Instance Scheduler): Pokud víš, že tvých 20 uživatelů spí v noci (např. 00:00 – 06:00), můžeš instanci automaticky vypínat a ušetřit 25 % nákladů.

4. Architektura tvého řešení

Jelikož chceš kontejner, nejjednodušší cesta je:

    Kontejner 1 (AI Engine): Použiješ image Ollama. Je to standard pro lokální AI. Do něj nahraješ model (např. phi4mini). Ollama ti automaticky vystaví API na portu 11434.

    Kontejner 2 (Tvoje Appka): Tvá aplikace bude prostě posílat dotazy na vnitřní IP adresu prvního kontejneru.

Verdikt pro tvůj projekt:

Aby to „nesežralo kredity“, zvol instanci t4g.medium (cca 800 Kč měsíčně). Pokud chceš jít na absolutní minimum a stačí ti hloupější chatbot, vezmi t4g.small a model Llama 4 Scout (1B) (cca 400 Kč měsíčně).

Chceš, abych ti pomohl sestavit přímo ten docker-compose soubor, který by ti tyhle dva kontejnery (appku a AI engine) propojil?
