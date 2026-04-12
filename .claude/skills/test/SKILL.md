---
name: test
description: Runs unit tests (full suite or specific module), summarizes failures, and optionally reports coverage. Pairs with test-fixer agent for automated repair.
---

You are executing the **test** skill for hellogsm-server-25.

## Step 1 — Parse User Input

Determine the scope from the user's request:
- **Full suite**: no module specified → run all tests
- **Domain module**: e.g., "member", "oneseo" → filter by package
- **Specific class**: e.g., "CreateMemberServiceTest" → run single class

## Step 2 — Run Tests

### Full suite
```bash
./gradlew test
```

### Specific domain
```bash
./gradlew test --tests "team.themoment.hellogsmv3.domain.member.*"
./gradlew test --tests "team.themoment.hellogsmv3.domain.oneseo.*"
```

### Specific class
```bash
./gradlew test --tests "team.themoment.hellogsmv3.domain.member.service.CreateMemberServiceTest"
```

Capture full output. Parse for:
- Total tests run
- Number of failures
- Number of skipped
- Failure details (class, method, message, first 10 lines of stack trace)

## Step 3 — Parse Results

Locate test report XML files for detailed results:
```bash
find build/test-results -name "*.xml" | head -20
```

Parse failures from XML or Gradle output. For each failure, extract:
1. **Test class** (fully qualified)
2. **Test method** + `@DisplayName` label
3. **Failure type**: AssertionError, NullPointerException, UnnecessaryStubbingException, etc.
4. **Message**: expected vs actual
5. **Root file + line number**

## Step 4 — Coverage (Optional)

If the user asked for coverage:
```bash
./gradlew test jacocoTestReport
```

Then check:
```bash
find build/reports/jacoco -name "index.html" | head -5
```

Report:
- Line coverage %
- Branch coverage %
- Top 5 uncovered classes

## Step 5 — Output Report

### Pass case
```markdown
## Test Results

✅ All tests passed

| Metric | Value |
|--------|-------|
| Tests run | 47 |
| Passed | 47 |
| Failed | 0 |
| Skipped | 0 |
| Duration | 8.3s |
```

### Failure case
```markdown
## Test Results

❌ {N} test(s) failed

| Metric | Value |
|--------|-------|
| Tests run | 47 |
| Passed | 44 |
| Failed | 3 |
| Skipped | 0 |

### Failures

#### 1. CreateMemberServiceTest › Describe_execute › Context_with_duplicate › it_throws_expected_exception
**Type:** AssertionError  
**Message:** Expected ExpectedException to be thrown, but nothing was thrown  
**Location:** CreateMemberServiceTest.java:58

#### 2. ...
```

## Step 6 — Offer test-fixer

If there are failures:
```
{N} test(s) failed. Run test-fixer agent to automatically diagnose and fix them?
```

Wait for user confirmation.