---
name: commit
description: Git Flow-aware commit skill that auto-detects branch context, separates changes into logical units, and writes messages following project conventions loaded from .claude/rules/commit-convention.md
---

You are executing the **commit** skill for hellogsm-server-25.

## Step 1 — Load Commit Rules

Read `.claude/rules/commit-convention.md` and extract:
- Allowed commit types
- Allowed scopes
- Message format

Also read `.claude/skills/commit/references/scope-guide.md` for the type/scope selection table.

## Step 2 — Check Branch Context

```bash
git branch --show-current
git status --short
git diff HEAD --stat
```

### Git Flow Branch Rules

| Current branch | Action                                                                                                                                           |
|----------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| `develop`      | Warn the user that committing directly to `develop` is unusual. Suggest creating a feature branch first. Ask for confirmation before proceeding. |
| `main`         | STOP. Do not commit to main. Tell the user to create a branch.                                                                                   |
| `feature/*`    | Proceed normally                                                                                                                                 |
| `hotfix/*`     | Proceed normally                                                                                                                                 |
| Any other      | Proceed normally                                                                                                                                 |

If on `develop` and user confirms they want to commit directly, proceed.

## Step 3 — Analyze Changes

```bash
git diff HEAD
git diff --cached
```

Group changed files by logical unit:
- Files in the same domain (`member/`, `oneseo/`, `operation/`) belong together
- Infrastructure/config changes are separate from feature changes
- Test changes can be committed with the feature or separately (ask user preference)

If there are multiple logical groups, explain the grouping to the user and ask which to commit first or if they want all in one commit.

## Step 4 — Determine Type and Scope

Using the loaded rules and scope guide, determine:
- **type**: feat, fix, update, refactor, test, ci/cd, code, docs, chore
- **scope**: global, member, oneseo, operation, common

Rules for selection:
- If a new API endpoint or service was added → `feat`
- If existing behavior was changed → `update`
- If only structure changed, behavior identical → `refactor`
- If only tests added/fixed → `test`
- If build.gradle, docker, CI config changed → `chore` or `ci/cd`

## Step 5 — Stage Files

Stage only the files for the current logical commit unit:
```bash
git add src/main/java/team/themoment/hellogsmv3/domain/{scope}/...
```

Never use `git add -A` without reviewing what's included.

## Step 6 — Write and Execute Commit

Compose the commit message following the format: `{type}({scope}): {description}`

```bash
git commit -m "$(cat <<'EOF'
{type}({scope}): {Korean description}
EOF
)"
```

If Spotless blocks the commit (compileJava depends on spotlessApply), run:
```bash
./gradlew spotlessApply
git add -u  # re-stage formatted files
git commit -m "..."
```

## Step 7 — Report

```
## Commit Complete

Branch: feature/member-search
Commit: feat(member): 회원 검색 기능 추가

Staged files:
- src/main/java/.../MemberController.java
- src/main/java/.../SearchMemberService.java
- src/test/java/.../SearchMemberServiceTest.java

Remaining uncommitted changes: {list or "none"}
```

## Prohibited Patterns
- Do not force-push (`--force`)
- Do not use `--no-verify` to skip hooks
- Do not amend already-pushed commits
- Do not commit `.env`, credentials, or secrets
