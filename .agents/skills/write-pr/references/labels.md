# GitHub PR Label Selection Guide

## Type Labels
| Label | When to apply |
|-------|--------------|
| `feat` | New feature or API endpoint |
| `fix` | Bug fix |
| `update` | Enhancement to existing functionality |
| `refactor` | Restructuring without behavior change |
| `test` | Test files only |
| `chore` | Build config, dependencies |
| `docs` | Documentation only |

## Scope Labels
| Label | When to apply |
|-------|--------------|
| `member` | Changes in `domain/member/` |
| `oneseo` | Changes in `domain/oneseo/` |
| `operation` | Changes in operation/date domain |
| `global` | Changes in `global/` |

## Special Labels
| Label | When to apply |
|-------|--------------|
| `breaking-change` | API shape changed, entity column renamed/removed |
| `security` | Auth/authz change |
| `db-migration` | Entity field added/removed/renamed |

## Rules
1. Always apply one type label
2. Always apply one scope label
3. Apply `breaking-change` if API contracts or DB schema changed
4. Apply `db-migration` if any `@Column` was changed or field removed