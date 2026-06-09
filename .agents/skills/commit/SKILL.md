---
name: commit
description: Git Flow-aware commit skill that auto-detects branch context, separates changes into logical units, and writes messages following project conventions loaded from .agents/skills/commit/references/scope-guide.md
---

You are executing the **commit** skill for hellogsm-server-25.

## Step 1 — Load Commit Rules

This skill manages its own rules. Read `.agents/skills/commit/references/scope-guide.md` for the type/scope selection table.

### Commit Message Format
```
{type}({scope}): {Korean description}
```
- No capital letter at start of description
- Korean descriptions are standard
- No period at end
- Subject line ≤ 72 characters

### Allowed Types
`feat`, `fix`, `update`, `refactor`, `test`, `ci/cd`, `code`, `docs`, `chore`

### Allowed Scopes
`global`, `member`, `oneseo`, `operation`, `common`

## Step 2 — Check Branch Context

```bash
git branch --show-current
git status --short
git diff HEAD --stat
```

### Git Flow Branch Rules

| Current branch | Action                                                             |
|----------------|--------------------------------------------------------------------|
| `develop`      | Warn: direct commits to develop are unusual. Ask for confirmation. |
| `main`         | STOP. Do not commit to main. Tell user to create a branch.         |
| `feature/*`    | Proceed normally                                                   |
| `hotfix/*`     | Proceed normally                                                   |

## Step 3 — Analyze Changes

```bash
git diff HEAD
git diff --cached
```

Group changed files by logical unit:
- Files in the same domain (`member/`, `oneseo/`, `operation/`) belong together
- Infrastructure/config changes are separate from feature changes
- Test changes can be committed with the feature or separately

## Step 4 — Stage Files

Stage only the files for the current logical commit unit. Never use `git add -A` blindly.

## Step 5 — Write and Execute Commit

```bash
git commit -m "$(cat <<'EOF'
{type}({scope}): {Korean description}
EOF
)"
```

If Spotless blocks the commit, run:
```bash
./gradlew spotlessApply
git add -u
git commit -m "..."
```

## Step 6 — Report

```
## Commit Complete

Branch: {branch}
Commit: {type}({scope}): {description}

Staged files: {list}
Remaining uncommitted changes: {list or "none"}
```

## Prohibited Patterns
- Do not force-push
- Do not use `--no-verify`
- Do not amend already-pushed commits
- Do not commit `.env`, credentials, or secrets
