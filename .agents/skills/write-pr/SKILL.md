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
## 변경 사항

- {bullet summary of changes}

## 변경 이유

{why this change was needed}

## 테스트

- [ ] 단위 테스트 추가/수정 완료
- [ ] `./gradlew test` 통과 확인
- [ ] 로컬 서버 기동 및 수동 테스트 완료

## 체크리스트

- [ ] 컨벤션 준수 (jakarta.*, ExpectedException, @Valid 등)
- [ ] Swagger 어노테이션 추가 (신규 API의 경우)
- [ ] 불필요한 System.out.println 없음

🤖 Generated with Claude Code
```

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