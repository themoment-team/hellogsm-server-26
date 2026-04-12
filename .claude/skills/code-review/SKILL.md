---
name: code-review
description: Checklist-based code review of git diff changes, loading rules dynamically from .claude/rules/. Outputs a ✓/⚠/✗ formatted report and triggers convention-validator for auto-fixable violations.
---

You are executing the **code-review** skill for hellogsm-server-25.

## Step 1 — Load All Rules

Read every file in `.claude/rules/`:
```bash
ls .claude/rules/
```
Read each `.md` file. Extract enforceable checklist items per category.

## Step 2 — Get Changed Files

```bash
git diff HEAD --name-only --diff-filter=ACM
```

If empty, check staged:
```bash
git diff --cached --name-only --diff-filter=ACM
```

Read the full diff:
```bash
git diff HEAD
```

Filter to Java source files only. Skip: `build/`, `**/Q*.java`, `*.xml`, `*.yml`.

## Step 3 — Review Each File

For each changed `.java` file, evaluate the following checklist (sourced from loaded rules):

### A. Coding Style `[coding-style.md]`
- [ ] Correct class-level annotations for layer type (Controller/Service/Entity)
- [ ] No `@Setter` on entity classes
- [ ] DTOs are records where applicable
- [ ] No `javax.*` imports (must be `jakarta.*`)
- [ ] Lombok used correctly (`@RequiredArgsConstructor`, no manual constructors)
- [ ] Import order (Spotless groups): java → javax → org → com → blank (jakarta.* falls in blank group)

### B. Logging `[logging.md]`
- [ ] No `System.out.println`
- [ ] No `e.printStackTrace()`
- [ ] All log calls use parameterized form (no string concatenation)
- [ ] `@Slf4j` present only when `log.` is actually called

### C. Exception Handling `[exception-handling.md]`
- [ ] No `throw new RuntimeException(...)` — uses `ExpectedException`
- [ ] No empty catch blocks
- [ ] No try/catch in controller methods
- [ ] No new custom exception subclasses

### D. API Conventions `[api-convention.md]`
- [ ] Endpoint path contains `/v3`
- [ ] Write methods return `CommonApiResponse` (not `ResponseEntity`) — read methods return DTO directly
- [ ] No `ResponseEntity` wrapper (not used in this project)
- [ ] `CommonApiResponse.success()` / `.created()` called with message only — no data argument
- [ ] `@RequestBody` parameters have `@Valid`
- [ ] Swagger `@Operation` + `@Tag` annotations present on new endpoints

### E. Testing `[testing.md]`
- [ ] New service classes have corresponding test files
- [ ] Test uses Describe/Context/It nested structure
- [ ] BDD-style stubbing (`given(...)`)
- [ ] No `@SpringBootTest` for unit tests

### F. Commit Convention `[commit-convention.md]`
- [ ] Latest commit message matches `{type}({scope}): {description}` format
- [ ] Type is from the allowed list
- [ ] Scope matches a known domain

### G. Security Basics
- [ ] No hardcoded credentials, tokens, or passwords
- [ ] No sensitive data in log statements
- [ ] SQL operations use JPA/QueryDSL (no raw string concatenation)
- [ ] `@PreAuthorize` or security annotations present where access control is needed

## Step 4 — Produce Report

```markdown
## Code Review Report

**Branch:** {branch name}  
**Changed files:** {N}  
**Review date:** {today}

### {FileName}.java

| Category | Item | Status | Notes |
|----------|------|--------|-------|
| Coding Style | No javax.* imports | ✓ | |
| Logging | Parameterized log calls | ✓ | |
| Exception | Uses ExpectedException | ✗ | Line 42: throw new RuntimeException(...) |
| API | @Valid on @RequestBody | ⚠ | Missing on CreateMemberReqDto parameter |
| Testing | New test file exists | ✓ | |
| Security | No hardcoded secrets | ✓ | |

### Overall Status

| Category | ✓ Pass | ⚠ Warning | ✗ Fail |
|----------|--------|-----------|--------|
| Coding Style | 5 | 1 | 0 |
| ...      | ...    | ...       | ...    |

**✗ Violations (must fix):** N  
**⚠ Warnings (should fix):** N
```

## Step 5 — Offer Auto-Fix

If there are any ✗ violations in Coding Style, Logging, or Exception Handling:
```
Auto-fixable violations found. Run convention-validator to apply fixes automatically?
```

Wait for user confirmation before triggering convention-validator.