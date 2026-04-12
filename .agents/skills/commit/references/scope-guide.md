# Commit Type & Scope Selection Guide

## Type Selection Table

| Situation | Type | Example |
|-----------|------|---------|
| New API endpoint, new service class, new feature | `feat` | `feat(oneseo): 원서 접수 기간 설정 기능 추가` |
| Bug fix | `fix` | `fix(member): 전화번호 중복 검사 조건 수정` |
| Enhancement of existing feature, dependency upgrade | `update` | `update(global): Spring Boot 버전 변경` |
| Code restructuring, no behavior change | `refactor` | `refactor(oneseo): QueryDSL 쿼리 메서드 분리` |
| Test additions or fixes only | `test` | `test(member): CreateMemberService 단위 테스트 추가` |
| Docker, CI/CD pipelines | `ci/cd` | `ci/cd(global): Docker Java 버전 변경` |
| Dead code removal, cleanup | `code` | `code(refactor): 미사용 메서드 제거` |
| Documentation changes only | `docs` | `docs(global): CLAUDE.md 규칙 섹션 추가` |
| Gradle config, dependency management | `chore` | `chore(global): AWS BOM 버전 변경` |

## Scope Selection Table

| Scope | What it covers |
|-------|---------------|
| `global` | Shared infra, security config, global exception handler, logging |
| `member` | Member entity, CRUD services, controller, DTOs |
| `oneseo` | Application form entity, services, controller, DTOs, custom queries |
| `operation` | Test result announcements, schedule/date operations |
| `common` | Cross-cutting domain utilities |

## Composite Changes

Use the primary affected scope. If truly cross-cutting, use `global`.

## Breaking Down Large Changes

When changes span 3+ unrelated areas, split into multiple commits:
1. Infrastructure/config first
2. Domain logic next
3. Tests last (or together with domain)