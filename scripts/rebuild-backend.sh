#!/usr/bin/env bash
# Rebuild backend without pulling from Docker Hub (uses cached maven image).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$ROOT/backend"
COMPOSE="docker compose -f $ROOT/docker-compose.yml"

echo "==> Building JAR inside cached maven container..."
docker run --rm \
  -v "$BACKEND:/app" -w /app \
  maven:3.9-eclipse-temurin-17 \
  mvn -q -DskipTests package

JAR=$(ls "$BACKEND"/target/opspulse-*.jar | head -1)
echo "==> Built $JAR"

if $COMPOSE ps backend 2>/dev/null | grep -q Up; then
  echo "==> Hot-swapping JAR into running container..."
  CID=$($COMPOSE ps -q backend)
  docker cp "$JAR" "$CID:/app/app.jar"
  $COMPOSE restart backend
  echo "==> Done. API: http://localhost:8090/api/health"
else
  echo "==> No running backend. Start with: docker compose up -d"
fi
