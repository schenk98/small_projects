#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

if ! command -v docker >/dev/null 2>&1; then
  echo "Required command 'docker' is missing from PATH."
  exit 1
fi

if ! command -v mvn >/dev/null 2>&1; then
  echo "Required command 'mvn' is missing from PATH."
  exit 1
fi

if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is not running. Start Docker Desktop and run again."
  exit 1
fi

wait_container_healthy() {
  local container_name="$1"
  local timeout_seconds="${2:-120}"
  local elapsed=0
  while [ "$elapsed" -lt "$timeout_seconds" ]; do
    local status
    status="$(docker inspect --format '{{.State.Health.Status}}' "$container_name" 2>/dev/null || true)"
    if [ "$status" = "healthy" ]; then
      echo "Container '$container_name' is healthy."
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  echo "Timeout waiting for container '$container_name' to become healthy."
  exit 1
}

echo "Starting database containers..."
docker compose up -d mysql mongo

echo "Waiting for MySQL and MongoDB health checks..."
wait_container_healthy "the-arena-mysql" 180
wait_container_healthy "the-arena-mongo" 180

if [ "${1:-}" != "--skip-build" ]; then
  echo "Running tests before startup..."
  mvn test
fi

echo "Starting The Arena application with docker profile..."
echo "Press Ctrl+C to stop the app. Databases stay up."
mvn spring-boot:run -Dspring-boot.run.profiles=docker
