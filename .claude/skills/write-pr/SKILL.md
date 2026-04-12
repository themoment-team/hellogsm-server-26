---
name: write-pr
description: Analyzes commits since branching from base, generates PR title/body/labels following project conventions, and creates the PR via GitHub CLI.
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
- Which domains changed (`member/`, `oneseo/`, `operation/`, `global/`)
- What was added vs modified vs deleted
- Whether tests were added
- Whether documentation was updated

## Step 3 — Load Commit Convention

Read `.claude/rules/commit-convention.md` to determine correct PR title format.
Read `.claude/skills/write-pr/references/labels.md` for label selection.

## Step 4 — Compose PR Title

Format: `{type}({scope}): {Korean description}`

Rules:
- If all commits share the same type → use that type
- If commits span multiple types → use the most significant one (`feat` > `fix` > `update` > `refactor`)
- Scope = primary domain affected
- Title ≤ 72 characters

## Step 5 — Compose PR Body

```markdown
## 개요

{작업 내용을 1~3 문장으로 요약}

## 본문

{변경 사항을 더 자세하게 서술 — 왜 이 변경이 필요했는지, 어떤 문제를 해결하는지 포함}

### 추가

{기존에 없던 무언가(기능, 코드 등)가 추가된 경우에만 작성. 없으면 섹션 전체 생략}

- {추가된 항목 bullet}

### 변경

{기존에 있던 무언가가 변경된 경우에만 작성. 없으면 섹션 전체 생략}

- {변경된 항목 bullet}

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

Rules for body composition:
- `### 추가` section: include only when new files, features, or endpoints are added
- `### 변경` section: include only when existing behavior, config, or code is modified
- If only additions exist, omit `### 변경` and vice versa
- Write in Korean

## Step 6 — Select Labels

Read `.claude/skills/write-pr/references/labels.md`. Select all applicable labels.

## Step 7 — Check Remote & Create PR

```bash
# Push if not yet pushed
git push -u origin $(git branch --show-current)
```

Create PR with gh CLI (preferred — body passed directly):
```bash
gh pr create \
  --base develop \
  --title "{title}" \
  --body "$(cat <<'EOF'
{body content here}
EOF
)" \
  --label "{label1}" \
  --label "{label2}"
```

Alternatively, use the helper script (body passed as 4th argument):
```bash
bash .claude/skills/write-pr/scripts/create-pr.sh \
  "{title}" \
  "develop" \
  "{label1,label2}" \
  "{body content here}"
```

## Step 8 — Output

```
## PR Created

Title: feat(oneseo): 원서 접수 기간 설정 기능 추가
Base: develop ← feature/feat/oneseo-period
Labels: feat, oneseo
URL: https://github.com/themoment-team/hellogsm-server-25/pull/341
```

## Prohibited Patterns
- Do not merge the PR
- Do not force-push to prepare for PR
- Do not create PR against `main` unless it's a hotfix branch