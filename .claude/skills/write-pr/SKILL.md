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

## Step 3 — Load PR Convention

Read `.claude/rules/pr-convention.md` to determine correct PR title format.

**Important:** PR titles do **not** follow commit convention. Do not read `commit-convention.md` for the title format.

## Step 4 — Compose PR Title

Format: `[{scope}] {Korean description}`

Rules:
- `scope` is a single lowercase token (`/` and `-` allowed, e.g. `ci/cd`)
- Discover domain scopes dynamically from `src/main/java/team/themoment/hellogsmv3/domain/` — do not rely on a hardcoded list
- Fixed scopes: `global` (cross-cutting/infra/security), `ci/cd` (pipelines/Docker/Actions)
- If changes span multiple domains → always use `[global]` (do not pick the "biggest" domain)
- Title ≤ 72 characters, Korean, no trailing period
- **Never** use commit-style `{type}({scope}):` for PR titles (e.g. `test(oneseo): ...` is wrong)

Examples (real merged PRs from this repo):
- `[oneseo] 인적사항 수정 API 추가`
- `[global] 테스트 코드 스타일 및 명칭 표준화`
- `[oneseo] 성취점수 리스트 내 null 요소로 인한 NPE 수정`

## Step 5 — Compose PR Body

Follow the structure defined in @.github/PULL_REQUEST_TEMPLATE.md.

Rules for body composition:
- `### 추가` section: include only when new files, features, or endpoints are added
- `### 변경` section: include only when existing behavior, config, or code is modified
- If only additions exist, omit `### 변경` and vice versa
- Write in Korean

## Step 6 — Create PR via Script

**Do not call `gh pr create` directly.** Use the bundled script — it handles pushing, validates the title against the PR convention, and centralizes the `gh` invocation:

```bash
bash .claude/skills/write-pr/scripts/create-pr.sh "<title>" "<base>" "<body>"
```

- `<base>` is `develop` (or `main` for hotfix branches)
- `<title>` must match `^\[[a-z][a-z0-9/-]*\] .+` — script will reject otherwise
- `<body>` can also be piped via stdin if it contains shell-troublesome characters:
  ```bash
  printf '%s' "$BODY" | bash .claude/skills/write-pr/scripts/create-pr.sh "<title>" "<base>"
  ```

The script will:
1. Push the current branch to origin if not yet pushed
2. Validate the title format (exit 1 with guidance on mismatch)
3. Run `gh pr create` with the validated inputs
4. Print the created PR URL

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
- Do not use commit-style `{type}({scope}): ...` for PR titles — use `[{scope}] ...`
- Do not call `gh pr create` directly — always go through `scripts/create-pr.sh`
