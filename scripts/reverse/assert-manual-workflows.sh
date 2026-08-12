#!/usr/bin/env bash
set -euo pipefail

if grep -REn '^[[:space:]]*(push|pull_request|schedule):|^[[:space:]]*on:[[:space:]].*(push|pull_request|schedule)' .github/workflows; then
  echo "Automatic GitHub Actions triggers are forbidden in the Reverse branch." >&2
  exit 1
fi
