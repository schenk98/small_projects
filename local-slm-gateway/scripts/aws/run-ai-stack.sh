#!/usr/bin/env bash
set -euo pipefail

# One-command AI stack (from local-slm-gateway on the AI EC2).
# Prereq: gateway.env next to docker-compose.yml (copy from gateway.env.example).

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

if [[ ! -f gateway.env ]]; then
  echo "Missing gateway.env in $ROOT"
  echo "Copy gateway.env.example to gateway.env and set AI_GATEWAY_API_KEY and OLLAMA_MODEL."
  exit 1
fi

MODEL="$(grep -E '^OLLAMA_MODEL=' gateway.env 2>/dev/null | head -1 | cut -d= -f2- | tr -d '\r' || true)"
MODEL="${MODEL:-phi4-mini}"

echo "==> Starting Ollama + gateway..."
docker compose up -d --build

echo "==> Pulling model '${MODEL}' (first time can take several minutes)..."
docker compose exec -T ollama ollama pull "${MODEL}"

echo "==> Done."
docker compose ps
echo "Health: curl -sS http://localhost:8090/health"
