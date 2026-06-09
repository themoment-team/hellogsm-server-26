---
name: code-review
description: Checklist-based code review of git diff changes for hellogsm-server-25 (Spring Boot 4 / Java 25). Outputs a ✓/⚠/✗ formatted report covering style, logging, exceptions, API conventions, tests, and security basics.
---

You are executing the **code-review** skill for hellogsm-server-25 (Spring Boot 4 / Java 25 / JUnit 5 / Mockito).

## Step 1 — Get Changed Files

```bash
git diff HEAD --name-only --diff-filter=ACM
git diff HEAD
```

Filter to `.java` files. Skip `build/`, `**/Q*.java`.

## Step 2 — Review Checklist

For each changed `.java` file:

### A. Coding Style
- [ ] Controller: `@Tag`, `@RestController`, `@RequestMapping`, `@RequiredArgsConstructor`
- [ ] Service: `@Service`, `@RequiredArgsConstructor`; `@Slf4j` only if `log.` used
- [ ] Entity: `@Getter`, `@Builder`, `@Entity`, `@Table(name="tb_...")`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
- [ ] No `@Setter` on entity
- [ ] DTOs are records
- [ ] No `javax.*` imports (use `jakarta.*`)

### B. Logging
- [ ] No `System.out.println`
- [ ] No `e.printStackTrace()`
- [ ] Parameterized log calls only (no string concatenation)
- [ ] `@Slf4j` only present when `log.` is actually used

### C. Exception Handling
- [ ] No `throw new RuntimeException(...)` — use `ExpectedException`
- [ ] No empty catch blocks
- [ ] No try/catch in controller methods
- [ ] No new exception subclasses

### D. API Conventions
- [ ] Endpoint path contains `/v3`
- [ ] Write methods return `CommonApiResponse` (not `ResponseEntity`) — read methods return DTO directly
- [ ] No `ResponseEntity` wrapper (not used in this project)
- [ ] `CommonApiResponse.success()` / `.created()` called with message only — no data argument
- [ ] `@RequestBody` has `@Valid`
- [ ] New endpoints have `@Operation` + `@Tag` Swagger annotations (Korean)

### E. Testing
- [ ] New services have corresponding test files
- [ ] Tests use Describe/Context/It nested structure with `@DisplayName`
- [ ] BDD-style stubbing: `given(mock.method()).willReturn(value)`
- [ ] No `@SpringBootTest` in unit tests

### F. Commit Convention
- [ ] Latest commit: `{type}({scope}): {Korean description}` format
- [ ] Type from: feat/fix/update/refactor/test/ci/cd/code/docs/chore

### G. Security Basics
- [ ] No hardcoded credentials/tokens
- [ ] No sensitive data in log statements
- [ ] JPA/QueryDSL used for queries (no raw string concatenation)

## Step 3 — Output Report

```markdown
## Code Review Report

### {FileName}.java

| Category | Item | Status | Notes |
|----------|------|--------|-------|
| Coding Style | No javax.* imports | ✓ | |
| Exception | Uses ExpectedException | ✗ | Line 42: throw new RuntimeException |

### Overall
| Category | ✓ | ⚠ | ✗ |
|----------|---|---|---|
| Coding Style | N | N | N |

**✗ Violations (must fix):** N  
**⚠ Warnings (should fix):** N
```