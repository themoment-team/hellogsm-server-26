---
name: write-pr
description: Analyzes commits since branching from base, generates PR title/body following project conventions, and creates the PR via GitHub CLI.
---

You are executing the **write-pr** skill for hellogsm-server-25.

## Step 1 — Determine Base Branch

```bash
git branch --show-current
git log --oneline origin/develop..HEAD
```

Default base is `develop`. If on a hotfix branch, base is `main`.

Confirm with user if base branch is unclear.

## Step 2 — Analyze Commits

```bash
# All commits since branching from base
git log origin/develop..HEAD --oneline
git diff origin/develop...HEAD --stat
git diff origin/develop...HEAD
```

Read the full diff to understand:
- Which modules/packages changed (explore the diff to discover affected domains dynamically)
- What was added vs modified vs deleted
- Whether tests were added
- Whether documentation was updated

## Step 3 — Load Commit Convention

Read `.claude/rules/commit-convention.md` to determine correct PR title format.

## Step 4 — Compose PR Title

Format: `{type}({scope}): {Korean description}`

Rules:
- If all commits share the same type → use that type
- If commits span multiple types → use the most significant one (`feat` > `fix` > `update` > `refactor`)
- Scope = primary domain affected
- Title ≤ 72 characters

## Step 5 — Compose PR Body

Follow the structure defined in @.github/PULL_REQUEST_TEMPLATE.md.

Rules for body composition:
- `### 추가` section: include only when new files, features, or endpoints are added
- `### 변경` section: include only when existing behavior, config, or code is modified
- If only additions exist, omit `### 변경` and vice versa
- Write in Korean

## Step 6 — Check Remote & Create PR

```bash
# Push if not yet pushed
git push -u origin $(git branch --show-current)
```

Create PR with gh CLI:
```bash
gh pr create \
  --base develop \
  --title "{title}" \
  --body "$(cat <<'EOF'
{body content here}
EOF
)"
```

## Step 7 — Output

After creating the PR, output the result in this format:

```
## PR Created

Title: {title}
Base:  develop ← {current-branch}
URL:   {pr-url}
```

## Prohibited Patterns
- Do not merge the PR
- Do not force-push to prepare for PR
- Do not create PR against `main` unless it's a hotfix branch
