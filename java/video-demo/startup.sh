#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
backend_dir="$root_dir/backend"
frontend_dir="$root_dir/frontend"

if [[ ! -d "$backend_dir" ]]; then
  echo "backend/ directory not found; cannot start backend." >&2
  exit 1
fi

echo "Starting backend..."
(cd "$backend_dir" && mvn spring-boot:run) &
backend_pid=$!

echo "Waiting for backend to be ready..."
max_attempts=60
attempt=0
while [ $attempt -lt $max_attempts ]; do
  if curl -s http://localhost:8080/api/videos > /dev/null 2>&1; then
    echo "Backend is ready!"
    break
  fi
  attempt=$((attempt + 1))
  sleep 1
done

if [ $attempt -eq $max_attempts ]; then
  echo "Warning: Backend did not become ready within $max_attempts seconds, starting frontend anyway..."
fi

frontend_pid=""
if [[ -d "$frontend_dir" && -f "$frontend_dir/package.json" ]]; then
  if [[ -d "$frontend_dir/node_modules" ]]; then
    echo "Starting frontend..."
    (cd "$frontend_dir" && npm run dev -- --port 5173) &
    frontend_pid=$!
  else
    echo "frontend/ found but dependencies are missing; run npm install inside frontend/."
  fi
fi

if [[ -n "$frontend_pid" ]]; then
  wait "$backend_pid" "$frontend_pid"
else
  wait "$backend_pid"
fi
