#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
project="dietreminder-production-test-${GITHUB_RUN_ID:-$$}"
repo_root="$PWD"
override_file="$(mktemp /tmp/dietreminder-production-XXXXXX.yaml)"
export DB_PASSWORD=production-smoke-test-only WEB_PORT=0 WEB_SECURE_COOKIES=false
cat > "$override_file" <<YAML
services:
  backend:
    image: ${project}-backend:local
    build:
      context: ${repo_root}
      dockerfile: backend/Dockerfile
  web:
    image: ${project}-web:local
    build:
      context: ${repo_root}
      dockerfile: web/Dockerfile
YAML
compose=(docker compose --project-name "$project" --env-file .env.production.example -f compose.production.yaml -f "$override_file")
cleanup() {
  result=$?
  trap - EXIT
  "${compose[@]}" logs --no-color || true
  "${compose[@]}" down --volumes --remove-orphans || true
  rm -f "$override_file"
  exit "$result"
}
trap cleanup EXIT
"${compose[@]}" up -d --build --wait --wait-timeout 180
address="http://$("${compose[@]}" port web 8080)"
curl --fail --silent --output /dev/null "$address/"
curl --fail --silent "$address/healthz"
curl --fail --silent "$address/api/app/version-policy"
curl --fail --silent "$address/api/auth/providers"
for service in backend postgres; do
  container_id="$("${compose[@]}" ps -q "$service")"
  test "$(docker inspect --format '{{len .HostConfig.PortBindings}}' "$container_id")" = 0
done
# Nginx must rediscover the backend container after replacement, without restarting web.
"${compose[@]}" up -d --no-deps --force-recreate --wait backend
curl --fail --silent --retry 15 --retry-all-errors --retry-delay 1 "$address/healthz"
printf '\nProduction smoke test passed: one entry point, internal backend/database, backend recreation.\n'
