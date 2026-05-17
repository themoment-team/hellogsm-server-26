#!/usr/bin/env bash
# create-pr.sh — Creates a GitHub PR via gh CLI, enforcing the project's PR title convention.
# Usage: bash create-pr.sh "<title>" "<base>" "<body>"
#   or pipe body via stdin:
#   echo "<body>" | bash create-pr.sh "<title>" "<base>"
#
# Title convention: [{scope}] {Korean description}
#   scope is a single lowercase token. Examples observed in this repo:
#     global, oneseo, member, operation, common, auth, ci/cd
#   New scopes are allowed — discover them from the domain directory.
# See .claude/rules/pr-convention.md for the full ruleset.

set -euo pipefail

TITLE="${1:?PR title is required}"
BASE="${2:-develop}"
BODY="${3:-}"

# Format check only: [<lowercase-token>] <description>
# Token allows letters, digits, '/' and '-' (e.g. ci/cd, sub-domain).
TITLE_REGEX='^\[[a-z][a-z0-9/-]*\] .+'

if [[ ! "$TITLE" =~ $TITLE_REGEX ]]; then
  echo "Error: PR title does not match project convention." >&2
  echo "  Got:      $TITLE" >&2
  echo "  Expected: [<scope>] <Korean description>" >&2
  echo "  Common scopes: global | oneseo | member | operation | common | auth | ci/cd" >&2
  echo "  Example:  [oneseo] 인적사항 수정 API 추가" >&2
  echo "" >&2
  echo "Note: commit-style titles like 'test(oneseo): ...' are NOT allowed for PRs." >&2
  echo "See .claude/rules/pr-convention.md for details." >&2
  exit 1
fi

if [[ ${#TITLE} -gt 72 ]]; then
  echo "Warning: PR title is longer than 72 characters (${#TITLE})." >&2
fi

CURRENT_BRANCH=$(git branch --show-current)

if [[ -z "$CURRENT_BRANCH" ]]; then
  echo "Error: not on a branch (detached HEAD?)." >&2
  exit 1
fi

if [[ "$CURRENT_BRANCH" == "main" || "$CURRENT_BRANCH" == "develop" ]]; then
  echo "Error: refusing to create PR from protected branch '$CURRENT_BRANCH'." >&2
  exit 1
fi

# Ensure branch is pushed
if ! git ls-remote --exit-code --heads origin "$CURRENT_BRANCH" > /dev/null 2>&1; then
  echo "Pushing branch to origin..."
  git push -u origin "$CURRENT_BRANCH"
fi

# If body not provided as argument, read from stdin (non-interactive only)
if [[ -z "$BODY" ]]; then
  if [ -t 0 ]; then
    echo "Error: PR body is required. Pass as 3rd argument or pipe via stdin." >&2
    exit 1
  fi
  BODY=$(cat)
fi

if [[ -z "${BODY// }" ]]; then
  echo "Warning: PR body is empty." >&2
fi

gh pr create \
  --base "$BASE" \
  --head "$CURRENT_BRANCH" \
  --title "$TITLE" \
  --body "$BODY"

echo ""
echo "PR created successfully."
echo "View: $(gh pr view --json url -q .url)"