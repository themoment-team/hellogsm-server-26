---
name: write-pr
description: Analyzes commits since branching from develop/main, generates PR title/body/labels following project conventions, and creates the PR via GitHub CLI for hellogsm-server-25.
---

You are executing the **write-pr** skill for hellogsm-server-25.

## Step 1 — Analyze Commits

```bash
git branch --show-current
git log origin/develop..HEAD --oneline
git diff origin/develop...HEAD --stat
git diff origin/develop...HEAD
```

Default base is `develop`. For hotfix branches, base is `main`.

## Step 2 — Compose PR Title

Format: `{type}({scope}): {Korean description}` (≤ 72 chars)

Types: `feat`, `fix`, `update`, `refactor`, `test`, `chore`, `ci/cd`  
Scopes: `global`, `member`, `oneseo`, `operation`, `common`

## Step 3 — Compose PR Body

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

🤖 Generated with Claude Code
```

Rules for body composition:
- `### 추가` section: include only when new files, features, or endpoints are added
- `### 변경` section: include only when existing behavior, config, or code is modified
- If only additions exist, omit `### 변경` and vice versa
- Write in Korean

## Step 4 — Select Labels

Read `.agents/skills/write-pr/references/labels.md` for label selection criteria.

## Step 5 — Create PR

```bash
git push -u origin $(git branch --show-current)

gh pr create \
  --base develop \
  --title "{title}" \
  --body "$(cat <<'EOF'
{body}
EOF
)" \
  --label "{label1}" \
  --label "{label2}"
```

## Step 6 — Output

```
## PR Created

Title: {title}
Base: develop ← {current-branch}
Labels: {labels}
URL: {url}
```