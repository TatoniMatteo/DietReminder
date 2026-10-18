#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew :common:check :client:check :backend:check :android:testDebugUnitTest :android:localCoverageReport :android:assembleDebug :android:assembleDebugAndroidTest :android:lintDebug
npm --prefix web run format:check
npm --prefix web run test:coverage
npm --prefix web run build
if [[ "${1:-}" == "--device" ]]; then
  ./gradlew :android:connectedDebugAndroidTest :android:createDebugAndroidTestCoverageReport
fi
# Browser tests require the local Docker distribution to be running.
python3 scripts/cleanup-test-accounts.py
trap 'python3 scripts/cleanup-test-accounts.py' EXIT
npm --prefix web run test:e2e
