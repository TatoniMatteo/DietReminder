#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Never reuse the developer's containers, images, database or account credentials.
project="dietreminder-e2e-${GITHUB_RUN_ID:-$$}-${GITHUB_RUN_ATTEMPT:-1}"
export DB_PORT="${E2E_DB_PORT:-15432}"
export BACKEND_PORT="${E2E_BACKEND_PORT:-18080}"
export WEB_PORT="${E2E_WEB_PORT:-18088}"
export MAIL_UI_PORT="${E2E_MAIL_PORT:-18025}"
export BACKEND_IMAGE="${project}-backend:local"
export WEB_IMAGE="${project}-web:local"
export WEB_TEST_URL="http://127.0.0.1:${WEB_PORT}"
export MAILPIT_TEST_URL="http://127.0.0.1:${MAIL_UI_PORT}"
unset E2E_ADMIN_EMAIL E2E_ADMIN_PASSWORD
export DB_NAME=dietreminder DB_USERNAME=dietreminder DB_PASSWORD=integration-tests-only
export BACKEND_BIND_ADDRESS=127.0.0.1 WEB_BIND_ADDRESS=127.0.0.1
export COMPOSE_PROFILES=dev WEB_SECURE_COOKIES=false
export SPRING_MAIL_HOST=mail SPRING_MAIL_PORT=1025 SMTP_AUTH=false SMTP_STARTTLS=false
export SPRING_MAIL_USERNAME= SPRING_MAIL_PASSWORD= AUTH_MAIL_FROM=e2e@dietreminder.local
export GOOGLE_CLIENT_ID= GOOGLE_CLIENT_SECRET= GOOGLE_TOKEN_ENCRYPTION_KEY=
export GOOGLE_DRIVE_REDIRECT_URI="${WEB_TEST_URL}/api/google/drive/callback"
docker_endpoint="${DOCKER_HOST:-$(docker context inspect --format '{{.Endpoints.docker.Host}}')}"
if [[ "$docker_endpoint" != unix://* ]]; then
  echo 'Integration tests require a local Docker Unix socket; remote Docker hosts are refused.' >&2
  exit 1
fi
compose=(docker --host "$docker_endpoint" compose --project-name "$project" --env-file .env.example -f compose.yaml -f compose.e2e.yaml)
cleanup() {
  result=$?
  trap - EXIT
  mkdir -p web/test-results
  "${compose[@]}" logs --no-color > web/test-results/stack.log 2>&1 || true
  "${compose[@]}" down --volumes --remove-orphans || true
  exit "$result"
}
trap cleanup EXIT
"${compose[@]}" up --build --wait --wait-timeout 180
curl --fail --silent "http://127.0.0.1:${BACKEND_PORT}/actuator/health/readiness"
curl --fail --silent --output /dev/null "http://127.0.0.1:${BACKEND_PORT}/v3/api-docs"
curl --fail --silent "${WEB_TEST_URL}/api/app/version-policy"
npm --prefix web run test:e2e -- "$@"
