#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT"

echo "==> Starting MongoDB + PostgreSQL + Mailhog(dev profile) + notification SOAP service via Docker Compose (from $ROOT)"
docker compose --profile dev up -d mongodb postgres mailhog notification-soap-service

echo "==> Waiting for local dependencies startup"
sleep 3

echo "==> Ensuring species shop items exist (non-destructive Mongo migration)"
docker exec poe-pet-mongodb mongosh -u admin -p admin123 --authenticationDatabase admin poe_pet /scripts/migrate-add-species-shop-items.js >/dev/null 2>&1 || true

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
  if [[ -f "./backend/mvnw" ]]; then
    (cd backend && ./mvnw -q -DskipTests compile)
  else
    (cd backend && mvn -q -DskipTests compile)
  fi
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
  if [[ -f "./backend/mvnw" ]]; then
    (cd backend && ./mvnw spring-boot:run ) &
  else
    (cd backend && mvn spring-boot:run ) &
  fi
else
  echo "==> Backend not found"
fi

echo ""
echo "==> Done. Frontend http://localhost:5173/  Backend http://localhost:8080/"
echo "    MailHog http://localhost:8025/  Notification SOAP WSDL http://localhost:8081/ws/notifications.wsdl"
echo "    Full container stack: docker compose --profile dev up -d --build"
echo "See README.md -> Verify local stack"
wait
