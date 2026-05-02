#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo "==> Starting MongoDB + Mailhog via Docker Compose (from $ROOT)"
docker compose up -d mongodb mailhog

echo "==> Waiting for MongoDB startup"
sleep 3

free_port() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    local p
    p="$(lsof -ti:"$port" 2>/dev/null || true)"
    if [[ -n "${p}" ]]; then
      echo "==> Stopping PID(s) on port $port: $p"
      kill -9 $p 2>/dev/null || true
    fi
  else
    echo "(lsof not found — skip freeing port $port)"
  fi
}

echo "==> Freeing dev ports 5173 and 8080 if possible"
free_port 5173
free_port 8080

if [[ -f "./backend/pom.xml" ]]; then
  echo "==> Compiling backend"
  (cd backend && mvn -q -DskipTests compile)
else
  echo "==> Backend not found"
fi

if [[ -f "./frontend/package.json" ]]; then
  echo "==> Starting frontend (background)"
  (
    cd frontend
    npm install
    npm run dev
  ) &
else
  echo "==> Frontend not initialized"
fi

if [[ -f "./backend/pom.xml" ]]; then
  echo "==> Starting backend (background)"
  (
    cd backend
    mvn spring-boot:run
  ) &
else
  echo "==> Backend not found"
fi

echo ""
echo "==> Done. Frontend http://localhost:5173/  Backend http://localhost:8080/"
echo "See README.md -> Verify local stack"
wait
