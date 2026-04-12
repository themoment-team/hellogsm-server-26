#!/usr/bin/env bash
# create-pr.sh — Creates a GitHub PR via gh CLI (for .agents/ system)
# Usage: bash create-pr.sh "<title>" "<base>" "<label1,label2>" "<body>"
#   or pipe body via stdin:
#   echo "<body>" | bash create-pr.sh "<title>" "<base>" "<label1,label2>"

set -euo pipefail

TITLE="${1:?PR title is required}"
BASE="${2:-develop}"
LABELS="${3:-}"
BODY="${4:-}"

CURRENT_BRANCH=$(git branch --show-current)

if ! git ls-remote --exit-code --heads origin "$CURRENT_BRANCH" > /dev/null 2>&1; then
  echo "Pushing branch to origin..."
  git push -u origin "$CURRENT_BRANCH"
fi

LABEL_FLAGS=""
if [[ -n "$LABELS" ]]; then
  IFS=',' read -ra LABEL_ARRAY <<< "$LABELS"
  for label in "${LABEL_ARRAY[@]}"; do
    LABEL_FLAGS="$LABEL_FLAGS --label $label"
  done
fi

if [[ -z "$BODY" ]]; then
  if [ -t 0 ]; then
    echo "Error: PR body is required. Pass as 4th argument or pipe via stdin." >&2
    exit 1
  fi
  BODY=$(cat)
fi

gh pr create \
  --base "$BASE" \
  --head "$CURRENT_BRANCH" \
  --title "$TITLE" \
  --body "$BODY" \
  $LABEL_FLAGS

echo "PR created: $(gh pr view --json url -q .url)"