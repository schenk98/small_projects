# Local SLM Gateway (side project)

Standalone, **cost-first** AI service intended to run on a cheap CPU-only AWS instance and be called by another app (e.g. your pet app).

Design goals:
- **Cheap over smart**: “stupid and slow but cheap” beats “fast and expensive”.
- **Private**: no public frontend; only an API callable by your pet backend (or local dev).
- **Runnable offline**: you can run the same stack on your own machine.

This folder is intentionally independent from the Java backend.

## What runs here

Two processes (Docker Compose):
- **Ollama**: local inference engine (downloads/hosts the model)
- **Gateway API** (Python/FastAPI): stable API contract for the caller and simple auth + cost guardrails

Why a gateway instead of calling Ollama directly?
- We can keep a stable `POST /v1/chat` contract even if we swap models/runtimes later.
- We can enforce **cost guardrails** (max prompt length, max tokens, timeouts).
- We can return a clear error when the model is down (the caller app owns the user-facing fallback).

## Quickstart (local)

Prereqs:
- Docker Desktop

From the `local-slm-gateway` folder:

```powershell
docker compose up -d
```

Then open:
- Gateway health: `http://localhost:8090/health`
- Readiness (checks Ollama reachable): `http://localhost:8090/ready`

### Set an API key (recommended)

Create a `gateway.env` file next to `docker-compose.yml`:

```env
AI_GATEWAY_API_KEY=dev-secret
OLLAMA_MODEL=phi4-mini
AI_OLLAMA_TIMEOUT_MS=60000
```

## API contract (gateway)

- `POST /v1/chat`
  - Header: `Authorization: Bearer <AI_GATEWAY_API_KEY>`
  - Body:
    - `userId: string`
    - `contextPrefix: string` (the fixed “system/prefix” message provided by the caller)
    - `conversation: { role: "user" | "assistant", content: string }[]`
    - `message: string` (the user message)

Response:
- `assistantText: string`
- `usage?: { latencyMs: number }`
- `fallbackUsed?: boolean`

Error responses:
- `401` if API key is missing/invalid
- `503` if Ollama/model is unavailable (caller should fall back to pet-specific noises)

## Cost-first knobs (important)

The gateway enforces:
- max prompt length (characters)
- max conversation turns included
- max output tokens
- request timeout to Ollama

These are intentionally conservative for CPU-only hosting.

## AWS deployment (EC2 + Docker Compose, cheapest path)

Target region: `eu-central-1` (Frankfurt).

Suggested starting point (CPU-only):
- `t4g.medium` (ARM, 2 vCPU / 4GB) for small models
- If too expensive, try `t4g.small` with a smaller model

Notes:
- Prefer **Spot** if you can tolerate occasional restarts.
- Put the instance in a security group that only allows inbound:
  - `22` from your IP (SSH)
  - `8090` only from your pet backend (or internal/VPC if you deploy both)

High-level steps:
1. Create EC2 instance (Amazon Linux 2023 ARM64 for `t4g.*`).
2. Install Docker + Docker Compose plugin.
3. Copy this folder onto the server (git clone or `scp`).
4. Create `.env` with a strong `AI_GATEWAY_API_KEY`.
5. Run `docker compose up -d`.

## Model choice (explicit research spike)

We have not committed to a model yet.

Selection rubric (in order):
1. **RAM footprint**
2. **CPU speed** for short replies
3. **License** suitability
4. **Quality** for “pet personality chat”
5. Operational ease (download size, startup time)

Keep models small (≤ ~4B) and prefer quantized variants when available.

## Pinning a model version (avoid `:latest`)

Ollama models are often referenced as `name:latest`. To avoid unexpected changes, create a pinned tag and use it in `gateway.env`.

Example (pins whatever you have locally right now):

```powershell
cd local-slm-gateway
docker compose exec -T ollama ollama cp phi4-mini:latest phi4-mini:pin-2026-05-07
```

Then set:
- `OLLAMA_MODEL=phi4-mini:pin-2026-05-07`

## Example caller prefix (pet app)

The caller (your pet app backend) should build a prefix like:

> You are cute pet named \<name\> of type \<dog\> that can magically speak. You are \<happy\>/\<happy_max\> happy, \<hunger\>/\<hunger_max\> fed and you have \<energy\>/\<energy_max\> energy. Respond on following message impersonating this character in language of that message: \<message\>

