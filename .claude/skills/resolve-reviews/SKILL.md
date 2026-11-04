---
name: resolve-reviews
description: Collect PR review comments, critically assess each one against project conventions, auto-apply valid ones, post refutation replies for invalid ones, and prompt for partial ones.
disable-model-invocation: true
allowed-tools: Bash(bash *get-pr-data.sh:*), Bash(gh api:*), Bash(gh pr view:*), Bash(gh repo:*), Bash(git add:*), Bash(git commit:*), Bash(git log:*), Bash(git push:*), Bash(git rev-parse:*), Bash(rm:*), Edit, Read
---

## Step 1 — Collect PR Data

```bash
bash "${CLAUDE_SKILL_DIR}/scripts/get-pr-data.sh"
```

Output files:
- `.pr-tmp/pr_comments.json` — inline review comments (id, path, line, body, user)
- `.pr-tmp/pr_changed_files.txt` — changed files
- `.pr-tmp/pr_commits.txt` — commits in this PR
- `.pr-tmp/pr_diff.txt` — full diff

Also fetch repo and PR metadata:

```bash
gh repo view --json nameWithOwner -q .nameWithOwner
gh pr view --json number,baseRefName -q '{number: .number, base: .baseRefName}'
```

## Step 2 — Load Rules and Assess Each Comment

Read existing `AGENTS.md` files that apply to each target path, from the repository root down to the target file's parent directory. More specific instructions override broader ones only within their subtree; do not apply a sibling directory's rules globally. Honor applicable instructions already supplied by the runtime.

`AGENTS.md` is optional. Read the other convention files listed below when they exist, and skip missing files or directories. Do not create a missing instruction file unless the user requests it. Cite only files and sections actually read. If no document covers a topic, use existing code and tool configuration as evidence and label inferred conventions separately from documented rules.

**Rule conflicts**: Follow the runtime instructions, path scopes, and any precedence explicitly defined by the project. Other convention documents are supporting references; do not invent a ranking between tool-specific guides and project documentation. If no applicable instruction resolves a conflict, cite both sources and leave the affected change unapplied while continuing independent work. Intentional scoped overrides are not contradictions.

Discover applicable `.claude/rules/**`, `.gemini/styleguide.md`, `CONTRIBUTING.md`, and `.github/copilot-instructions.md` files if present, and read them before judging comments. Existing code patterns can provide context but are not grounds to reject a review as a documented-rule violation.

For each comment in `pr_comments.json`, apply the following **layered judgment criteria**:

### Judgment criteria (priority order)

1. **Project conventions** (primary): apply rules discovered above
   - DTO annotation rules, commit scope, logging style, exception message format, etc.
2. **Language/framework best practices** (secondary): Kotlin official guide, Spring Boot recommendations
   - Apply only when no matching project rule exists

### Verdicts

- **VALID**: reviewer is correct → attempt auto code fix
- **INVALID**: reviewer is wrong with a clear refutation → skip, post refutation reply
- **PARTIAL**: intent is correct but application method or scope is ambiguous → confirm with AskUserQuestion

Always cite an actual source in the rationale (e.g. `AGENTS.md §Logging Style`, `Kotlin: prefer val over var`).

## Step 3 — Act on Each Verdict

### VALID → Auto fix

1. Read the target file with the Read tool
2. Apply the reviewer's concern with the Edit tool
3. If the changes have not been committed yet, commit them
4. Record the short commit hash for use in Step 5:
   ```bash
   git rev-parse --short=7 HEAD
   ```

On failure: record the reason and fall back to PARTIAL.

### INVALID → Skip

Do not modify any code. Record the refutation rationale for Step 5.

### PARTIAL → Confirm with AskUserQuestion

Use the AskUserQuestion tool to ask:

```
⚠️ PARTIAL: [file:line] (reviewer)
Review: "..."
Rationale: ...
Accept? (y / n / s = skip for now)
```

- `y`: treat as VALID, attempt code fix
- `n`: treat as INVALID, skip
- `s` / other: record as PENDING

## Step 4 — Print Report

```
## resolve-reviews Results

| # | Reviewer | File | Verdict | Rationale | Action |
|---|----------|------|---------|-----------|--------|
| 1 | alice | Foo.kt:12 | ✅ VALID | AGENTS.md §Logging Style | Auto-fixed (abc1234) |
| 2 | bob | Bar.kt:34 | ❌ INVALID | AGENTS.md §Exception Message | Skipped |
| 3 | alice | Baz.kt:56 | ⚠️ PARTIAL | - | PENDING |
```

## Step 5 — Push Commits

If any VALID fixes were committed in Step 3, push them before posting replies so the referenced commit hashes are visible on GitHub:

```bash
git push
```

## Step 6 — Post GitHub Replies

Post an inline reply for each comment. Always quote `path` and `comment_id` to prevent shell injection.

```bash
gh api "repos/<owner>/<repo>/pulls/<pr_number>/comments/<comment_id>/replies" \
  -f body="<reply_body>"
```

For reply body templates, read `${CLAUDE_SKILL_DIR}/references/reply-formats.md`.

## Step 7 — Cleanup

```bash
rm -rf .pr-tmp
```