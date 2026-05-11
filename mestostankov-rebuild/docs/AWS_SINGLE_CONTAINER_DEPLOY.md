## AWS “single container” deploy (MVP)

Goal: run the **Next.js frontend** + **TTS gateway** (Piper) behind one public endpoint on AWS.

### 0) Prerequisites

- AWS account + permissions to use ECR + ECS (Fargate)
- Docker Desktop installed locally
- A domain (optional for later HTTPS), e.g. `mestostankov.cz`

### 1) Create a single Docker image

This MVP currently has 2 runtimes:

- Node/Next.js (`product/frontend`)
- Python/FastAPI + Piper (`product/tts-gateway`)

For “single container”, we run both processes in one image using `supervisord`.

#### 1.1 Add a `deploy/` Dockerfile (recommended location)

Create `mestostankov-rebuild/deploy/Dockerfile` that:

- builds the Next.js app
- installs the TTS gateway + Piper + voice
- runs both with `supervisord`

*(If you want, I can implement this file in the repo next.)*

### 2) Push image to ECR

1. Create an ECR repo (example name `stankov-mvp`).
2. Authenticate Docker to ECR.
3. Build + tag + push:

```bash
docker build -t stankov-mvp -f mestostankov-rebuild/deploy/Dockerfile .
docker tag stankov-mvp:latest <ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com/stankov-mvp:latest
docker push <ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com/stankov-mvp:latest
```

### 3) Run on ECS Fargate

Create:

- ECS Cluster
- Task Definition (1 container)
- Service (desired count 1)

Expose port 3000 (Next.js). Internally the TTS gateway can listen on 8087.

Environment variables to set in the task:

- `SCRAPE_ORIGIN=https://www.mestostankov.cz`
- `SCRAPE_DATASET_ROOT=/app/data/scrape` *(only if you bake the dataset in the image)*
- `TTS_GATEWAY_URL=http://127.0.0.1:8087`

### 4) Load balancer + HTTPS

- Create an ALB in front of the service
- Add an ACM certificate for the domain
- Route53: point the domain to the ALB

### 5) Data strategy (important)

Right now the frontend reads the scrape dataset from disk.

You have 3 options:

1. **Bake dataset into the Docker image** (fastest MVP, but redeploy to update content)
2. **Mount EFS** and keep dataset there (updates without redeploy)
3. **Move to DB** (Supabase/Postgres) and import the scrape (roadmap “Phase 2”)

### 6) Smoke test checklist

- `/` loads
- `/mesto/soucasnost/` loads
- `/informace-pro-obcany/aktuality/` pagination works
- TTS button triggers `/api/tts` and plays audio
- Language buttons work and persist (cookie)

