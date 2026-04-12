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
## 변경 사항

- {bullet point summary of each logical change}
- {bullet point 2}

## 변경 이유

{Why this change was needed — fill from commit messages and diff context}

## 테스트

- [ ] 단위 테스트 추가/수정 완료
- [ ] `./gradlew test` 통과 확인
- [ ] 로컬 서버 기동 및 수동 테스트 완료

## 체크리스트

- [ ] `.claude/rules/` 컨벤션 준수
- [ ] Swagger 어노테이션 추가 (신규 API의 경우)
- [ ] 불필요한 `System.out.println` 없음
- [ ] `ExpectedException` 사용 (직접 `RuntimeException` 사용 안 함)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
```

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