#!/usr/bin/env bash
set -euo pipefail

# One-command pet stack (from repo root on the pet EC2).
# Prereq: copy documentation/aws/pet.aws.env.example to .env.aws and edit it.

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

if [[ ! -f .env.aws ]]; then
  echo "Missing .env.aws in $ROOT"
  echo "Copy documentation/aws/pet.aws.env.example to .env.aws and fill in secrets + URLs."
  exit 1
fi

echo "==> Building and starting stack (docker compose)..."
docker compose --env-file .env.aws -f docker-compose.yml up -d --build

echo "==> Done. Open WEB_BASE_URL from .env.aws (port 80 on this host)."
docker compose --env-file .env.aws -f docker-compose.yml ps
