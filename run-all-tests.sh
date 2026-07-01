#!/usr/bin/env bash
#
# Run all Portfolio project tests (unit, integration, gateway, Redis idempotency).
#
# Usage:
#   ./run-all-tests.sh              # full mvn verify
#   ./run-all-tests.sh --quick      # skip clean, faster re-run
#   ./run-all-tests.sh --redis-only # only Redis / multi-instance idempotency tests
#

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# ── Java 21 ──────────────────────────────────────────────────────────────────
if command -v /usr/libexec/java_home &>/dev/null; then
  export JAVA_HOME="${JAVA_HOME:-$(/usr/libexec/java_home -v 21 2>/dev/null || true)}"
fi

if [[ -z "${JAVA_HOME:-}" ]] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "21'; then
  echo "ERROR: Java 21 is required. Set JAVA_HOME or install JDK 21."
  exit 1
fi

echo "Using Java: $($JAVA_HOME/bin/java -version 2>&1 | head -1)"
echo "Project:    $SCRIPT_DIR"
echo ""

# ── Docker (optional — Redis tests skip if unavailable) ─────────────────────
DOCKER_AVAILABLE=false
if command -v docker &>/dev/null && docker info &>/dev/null 2>&1; then
  DOCKER_AVAILABLE=true
  echo "Docker:     available (Redis / multi-instance tests will run)"
else
  echo "Docker:     not available (Redis tests will be skipped via @EnabledIfDockerAvailable)"
fi
echo ""

# ── Parse args ───────────────────────────────────────────────────────────────
MVN_GOALS="clean verify"
REDIS_ONLY=false

for arg in "$@"; do
  case "$arg" in
    --quick)
      MVN_GOALS="verify"
      ;;
    --redis-only)
      REDIS_ONLY=true
      ;;
    -h|--help)
      echo "Usage: $0 [--quick] [--redis-only]"
      exit 0
      ;;
    *)
      echo "Unknown option: $arg"
      exit 1
      ;;
  esac
done

# ── Run tests ─────────────────────────────────────────────────────────────────
if [[ "$REDIS_ONLY" == true ]]; then
  echo "=== Redis / multi-instance idempotency tests ==="
  mvn -pl portfolio-performance test \
    -Dtest=RedisIdempotencyStoreTest,AttributionIdempotencyMultiInstanceTest
else
  echo "=== Full Maven verify (all modules) ==="
  mvn $MVN_GOALS
fi

echo ""
echo "=== Done ==="

if [[ "$DOCKER_AVAILABLE" == false && "$REDIS_ONLY" == true ]]; then
  echo "NOTE: Redis tests were likely skipped — start Docker Desktop and re-run."
  exit 0
fi

if [[ "$DOCKER_AVAILABLE" == false ]]; then
  echo "TIP: Start Docker Desktop and run:"
  echo "  ./run-all-tests.sh --redis-only"
fi
