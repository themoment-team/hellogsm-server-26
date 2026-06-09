#!/usr/bin/env bash
# create-pr.sh — Creates a GitHub PR via gh CLI (for .agents/ system)
# Usage: bash create-pr.sh "<title>" "<base>" "<body>"
#   or pipe body via stdin:
#   echo "<body>" | bash create-pr.sh "<title>" "<base>"

set -euo pipefail

TITLE="${1:?PR title is required}"
BASE="${2:-develop}"
BODY="${3:-}"

CURRENT_BRANCH=$(git branch --show-current)

if ! git ls-remote --exit-code --heads origin "$CURRENT_BRANCH" > /dev/null 2>&1; then
  echo "Pushing branch to origin..."
  git push -u origin "$CURRENT_BRANCH"
fi

if [[ -z "$BODY" ]]; then
  if [ -t 0 ]; then
    echo "Error: PR body is required. Pass as 3rd argument or pipe via stdin." >&2
    exit 1
  fi
  BODY=$(cat)
fi

gh pr create \
  --base "$BASE" \
  --head "$CURRENT_BRANCH" \
  --title "$TITLE" \
  --body "$BODY"

echo "PR created: $(gh pr view --json url -q .url)"
