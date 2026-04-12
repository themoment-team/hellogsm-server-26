# Commit Type & Scope Selection Guide

## Type Selection Table

| Situation | Type | Example |
|-----------|------|---------|
| New API endpoint, new service class, new feature | `feat` | `feat(oneseo): 원서 접수 기간 설정 기능 추가` |
| Bug fix, incorrect behavior corrected | `fix` | `fix(member): 전화번호 중복 검사 조건 수정` |
| Enhancement of existing feature, dependency upgrade | `update` | `update(global): Spring Boot 버전 변경` |
| Code restructuring, no behavior change | `refactor` | `refactor(oneseo): QueryDSL 쿼리 메서드 분리` |
| Test additions or fixes only | `test` | `test(member): CreateMemberService 단위 테스트 추가` |
| Docker, CI/CD pipelines, GitHub Actions | `ci/cd` | `ci/cd(global): Docker Java 버전 변경` |
| Dead code removal, unused import cleanup | `code` | `code(refactor): 미사용 메서드 제거` |
| Documentation changes only | `docs` | `docs(global): CLAUDE.md 규칙 섹션 추가` |
| Gradle config, dependency management (non-feature) | `chore` | `chore(global): AWS BOM 버전 변경` |

## Scope Selection Table

Two scopes are fixed regardless of code structure:

| Scope | What it covers |
|-------|---------------|
| `global` | Shared infra, security config, global exception handler, logging, response wrapper |
| `ci/cd` | CI/CD pipelines, Docker, GitHub Actions — always paired with `global` scope |

All other scopes correspond to **domain subdirectories** under `src/main/java/.../domain/`.
Discover them dynamically by listing that directory — do not rely on a hardcoded list.

> `domain/common/` contains sub-domains (e.g., `operation`, `date`). Use the specific sub-domain name as scope when the change is limited to it, or `common` when it is truly cross-cutting.

## Composite Changes

When a change touches multiple scopes, use the **primary affected scope**:

```
# Feature spans oneseo + member
feat(oneseo): 지원자 회원 정보 연동 기능 추가

# Affects global config AND oneseo
update(global): Redis 세션 설정 변경
```

If truly equal weight across scopes:
```
refactor(global): 공통 유틸리티 패키지 구조 개선
```

## Breaking Down Large Changes

When `git diff` shows changes in 3+ unrelated areas, split into multiple commits:

1. Infrastructure/config changes first: `chore(global): ...`
2. Domain logic next: `feat(oneseo): ...`
3. Tests last (or together with domain): `test(oneseo): ...`

Always ask the user before splitting if unclear.