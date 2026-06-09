---
name: security-checklist
description: Scans changed files for hardcoded secrets, SQL injection risks, missing auth/authz, and sensitive data in logs for hellogsm-server-25 (Spring Boot 4 / Java 25).
---

You are executing the **security-checklist** skill for hellogsm-server-25.

## Step 1 — Get Target Files

```bash
git diff HEAD --name-only --diff-filter=ACM
```

Filter to: `.java`, `.yml`, `.yaml`, `.properties`, `.xml`, `.gradle`.

## Step 2 — Run Security Scans

### Hardcoded Secrets
```bash
grep -rn "password\s*=\s*\"[^${\"]" src/ --include="*.java"
grep -rn "secret\s*=\s*\"[^${\"]" src/ --include="*.java"
grep -rn "token\s*=\s*\"[^${\"]" src/ --include="*.java"
grep -rn "password:" src/main/resources/ | grep -v "\${" | grep -v "#"
```

### SQL Injection
```bash
grep -rn "nativeQuery.*true" src/main/java/ --include="*.java" -A 3
grep -rn "createQuery.*+" src/main/java/ --include="*.java"
```

### Missing Auth
```bash
grep -rn "permitAll\(\)" src/main/java/ --include="*.java" -B 3
```

### Sensitive Data in Logs
```bash
grep -rn "log\.\(info\|warn\|error\).*password" src/main/java/ --include="*.java" -i
grep -rn "log\.\(info\|warn\|error\).*token" src/main/java/ --include="*.java" -i
```

### Insecure Patterns
```bash
grep -rn "ObjectInputStream" src/main/java/ --include="*.java"
grep -rn "allowedOrigins.*\*" src/main/java/ --include="*.java"
```

## Step 3 — Output Report

```markdown
## Security Checklist Report

### 🔴 CRITICAL
| Check | Location | Detail |

### 🟡 WARNING  
| Check | Location | Detail |

### ✅ PASSED
| Check | Result |
```

For CRITICAL findings: do NOT commit or push until resolved.