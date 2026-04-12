---
name: security-checklist
description: Scans changed files for hardcoded secrets, SQL injection risks, missing auth/authz, and sensitive data in logs. Uses grep-based pattern matching on git diff output.
---

You are executing the **security-checklist** skill for hellogsm-server-25.

## Step 1 — Identify Target Files

```bash
git diff HEAD --name-only --diff-filter=ACM
```

If empty, check staged:
```bash
git diff --cached --name-only --diff-filter=ACM
```

Filter to: `.java`, `.yml`, `.yaml`, `.properties`, `.xml`, `.gradle` files.

## Step 2 — Run Security Scans

### Check 1 — Hardcoded Secrets
```bash
# Passwords/secrets in code
grep -rn "password\s*=\s*\"[^${\"]" src/ --include="*.java"
grep -rn "secret\s*=\s*\"[^${\"]" src/ --include="*.java"
grep -rn "token\s*=\s*\"[^${\"]" src/ --include="*.java"
grep -rn "apiKey\s*=\s*\"[^${\"]" src/ --include="*.java"

# Hardcoded in YAML/properties
grep -rn "password:" src/main/resources/ | grep -v "\${" | grep -v "#"
grep -rn "secret:" src/main/resources/ | grep -v "\${" | grep -v "#"
```

### Check 2 — SQL Injection Risk
```bash
# String concatenation in queries (high risk)
grep -rn "nativeQuery.*true" src/main/java/ --include="*.java" -A 3
grep -rn "createQuery.*+" src/main/java/ --include="*.java"
grep -rn "createNativeQuery.*+" src/main/java/ --include="*.java"
```

### Check 3 — Missing Authentication / Authorization
```bash
# Endpoints without security annotations
grep -rn "@GetMapping\|@PostMapping\|@PutMapping\|@PatchMapping\|@DeleteMapping" \
  src/main/java/ --include="*.java" -B 5 | grep -v "@PreAuthorize\|@Secured\|@AuthRequest\|permitAll"

# Public endpoints (check SecurityConfig for intentional ones)
grep -rn "permitAll\(\)" src/main/java/ --include="*.java" -B 3
```

### Check 4 — Sensitive Data in Logs
```bash
# Password/token/resident-number in log statements
grep -rn "log\.\(info\|warn\|error\|debug\).*password" src/main/java/ --include="*.java" -i
grep -rn "log\.\(info\|warn\|error\|debug\).*token" src/main/java/ --include="*.java" -i
grep -rn "log\.\(info\|warn\|error\|debug\).*residentNumber\|주민\|jumin" src/main/java/ --include="*.java" -i
```

### Check 5 — Insecure Deserialization
```bash
# ObjectInputStream usage (potential deserialization vuln)
grep -rn "ObjectInputStream" src/main/java/ --include="*.java"
# Jackson type info (potential polymorphic deserialization)
grep -rn "@JsonTypeInfo\|enableDefaultTyping" src/main/java/ --include="*.java"
```

### Check 6 — Open Redirect
```bash
# Redirect with user-controlled input
grep -rn "redirect:.*{" src/main/java/ --include="*.java"
grep -rn "sendRedirect" src/main/java/ --include="*.java"
```

### Check 7 — CORS Configuration
```bash
# Wildcard CORS (should not be in production)
grep -rn "allowedOrigins.*\*\|setAllowedOrigins.*\*" src/main/java/ --include="*.java"
```

## Step 3 — Check Changed Files Specifically

For each file changed in the current diff, run targeted checks:
```bash
git diff HEAD -- {file_path}
```

Review the diff for:
- Any of the above patterns in added lines (prefix `+`)
- New endpoints that bypass security config
- New `@Value` injections replacing proper secret management

## Step 4 — Output Report

```markdown
## Security Checklist Report

**Scanned files:** {N} changed files + full codebase grep

### 🔴 CRITICAL — Fix Immediately

| Check | Location | Detail |
|-------|----------|--------|
| Hardcoded secret | src/.../Config.java:42 | `secret = "abc123"` |

### 🟡 WARNING — Review Required

| Check | Location | Detail |
|-------|----------|--------|
| Missing @AuthRequest | MemberController.java:55 | Admin endpoint may be publicly accessible |

### ✅ PASSED

| Check | Result |
|-------|--------|
| SQL Injection | No raw string concatenation in queries |
| Sensitive data in logs | No password/token in log statements |
| Hardcoded credentials in YAML | All values use ${...} placeholders |

### ℹ️ INFO — Informational

| Item | Detail |
|------|--------|
| Native queries found | 2 files use @Query(nativeQuery=true) — verify parameterization |
| Public endpoints | /member/v3/member/code (OAuth) — verified intentional in SecurityConfig |
```

## Step 5 — Next Steps

For CRITICAL findings:
```
🔴 Critical security issue found. Do NOT commit or push until resolved.
Suggested fix: [specific remediation]
```

For warnings, ask user if they want to proceed or address first.