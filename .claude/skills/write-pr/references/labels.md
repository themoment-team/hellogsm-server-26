# GitHub PR Label Selection Guide

## Type Labels

| Label | When to apply |
|-------|--------------|
| `feat` | New feature or new API endpoint added |
| `fix` | Bug fix — incorrect behavior corrected |
| `update` | Enhancement or change to existing functionality |
| `refactor` | Code reorganized without behavior change |
| `test` | Only test files changed |
| `chore` | Dependency updates, build config, CI/CD |
| `docs` | Documentation only |

## Scope Labels

| Label | When to apply |
|-------|--------------|
| `member` | Changes in `domain/member/` |
| `oneseo` | Changes in `domain/oneseo/` |
| `operation` | Changes in `domain/common/operation/` or `domain/common/date/` |
| `global` | Changes in `global/` (security, config, exception handling) |

## Size Labels (optional — apply if project uses them)

| Label | Criteria |
|-------|----------|
| `size/XS` | ≤ 10 lines changed |
| `size/S` | 11–50 lines |
| `size/M` | 51–200 lines |
| `size/L` | 201–500 lines |
| `size/XL` | > 500 lines |

## Special Labels

| Label | When to apply |
|-------|--------------|
| `breaking-change` | API response shape changed, entity column renamed/removed |
| `security` | Security fix or auth/authz change |
| `db-migration` | Entity field added/removed/renamed (Flyway migration needed) |
| `needs-review` | Non-trivial logic that requires careful human review |

## Selection Rules

1. Always apply one **type** label
2. Always apply one **scope** label (or `global` if cross-cutting)
3. Apply `breaking-change` if the change affects API contracts or DB schema
4. Apply `db-migration` if any `@Column` annotation was changed or entity field removed

## Example Combinations

| PR Type | Labels |
|---------|--------|
| New oneseo API endpoint | `feat`, `oneseo` |
| Fix member phone number validation | `fix`, `member` |
| Upgrade Spring Boot | `chore`, `global` |
| Remove deprecated QueryDSL query | `refactor`, `oneseo` |
| Add unit tests for CreateMemberService | `test`, `member` |
| Remove entity column | `update`, `oneseo`, `db-migration`, `breaking-change` |