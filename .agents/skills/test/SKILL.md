---
name: test
description: Runs unit tests for hellogsm-server-25 (full suite or specific module/class), summarizes failures with root cause analysis, and optionally reports coverage.
---

You are executing the **test** skill for hellogsm-server-25.

## Step 1 — Determine Scope

- Full suite: `./gradlew test`
- Domain: `./gradlew test --tests "team.themoment.hellogsmv3.domain.{domain}.*"`
- Class: `./gradlew test --tests "...{ClassName}"`

## Step 2 — Run Tests

```bash
./gradlew test
```

Parse for: total run, failures, skipped, and per-failure: class, method, type, message.

## Step 3 — Analyze Failures

| Failure Pattern | Likely Root Cause |
|-----------------|-------------------|
| `UnnecessaryStubbingException` | Service no longer calls stubbed method |
| `WantedButNotInvoked` | Service behavior changed |
| `NullPointerException` | Missing stub for new service dependency call |
| `AssertionFailedError: expected X but was Y` | Service return value changed |
| `cannot find symbol` | Method renamed/removed in service |

## Step 4 — Output Report

### All pass
```
✅ All {N} tests passed ({time}s)
```

### Failures
```markdown
## Test Results

❌ {N} test(s) failed

### Failure 1 — {TestClass} › {DisplayName}
**Type:** {ExceptionType}  
**Message:** {message}  
**Location:** {file}:{line}
```

## Step 5 — Offer Repair

If failures exist:
```
{N} test(s) failed. Diagnose and fix automatically? (uses test-fixer logic)
```