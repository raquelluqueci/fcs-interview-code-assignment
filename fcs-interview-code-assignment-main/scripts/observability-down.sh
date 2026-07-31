#!/usr/bin/env bash
# Stop the self-contained observability stack.
# Author: tfantas — https://www.tfantas.io — Lisbon, July 2026
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
docker compose -f docker-compose.observability.yml down
echo "OK: stack down"
