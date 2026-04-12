# CLAUDE.md — hellogsm-server-25

> Hello, GSM | 광주소프트웨어마이스터고등학교 입학 지원 서비스

## Project Overview

Spring Boot 4 / Java 25 REST API server for GSM school admissions.

| Item      | Value                                    |
|-----------|------------------------------------------|
| Language  | Java 25                                  |
| Framework | Spring Boot 4.0.5                        |
| Build     | Gradle                                   |
| ORM       | JPA + QueryDSL 7.1                       |
| DB        | MySQL + Redis                            |
| Auth      | Spring Security + OAuth2 (Google, Kakao) |
| Testing   | JUnit 5 + Mockito 5                      |

## Package Structure

```
src/main/java/team/themoment/hellogsmv3/
├── domain/
│   ├── member/        # 회원 도메인
│   ├── oneseo/        # 원서 도메인
│   └── common/        # 공통 도메인 (operation, date)
└── global/
    ├── exception/     # GlobalExceptionHandler, ExpectedException
    ├── config/        # Spring 설정
    ├── security/      # OAuth2, SecurityConfig
    └── common/        # CommonApiResponse, LoggingFilter
```

## Core Conventions (see `.claude/rules/` for full details)

### Response Wrapper
Controllers return `CommonApiResponse` for write operations, and the DTO directly for read operations.

```java
// Write operations (create / update / delete) — no data in response
public CommonApiResponse create(@RequestBody @Valid OneseoReqDto reqDto) {
    service.execute(reqDto);
    return CommonApiResponse.created("생성되었습니다.");
}

public CommonApiResponse update(@PathVariable Long memberId, @RequestBody @Valid OneseoReqDto reqDto) {
    service.execute(memberId, reqDto);
    return CommonApiResponse.success("수정되었습니다.");
}

// Read operations — return DTO directly
public FoundMemberResDto find(@AuthRequest Long memberId) {
    return service.execute(memberId);
}
```

`CommonApiResponse.success()` and `.created()` accept only a `String message` — there is no `(message, data)` overload.

### Exception Handling
Only one custom exception exists: `ExpectedException(message, HttpStatus)`.  
Never use `RuntimeException` directly.

### Logging
Use `@Slf4j` + parameterized logging. No `System.out.println`, no `e.printStackTrace()`.

### API Versioning
All endpoints use `/v3` suffix: `/{domain}/v3/{resource}`.

### Code Format
Run `./gradlew spotlessApply` before committing (auto-runs on compile).

## Detailed Guides (Skills)

Invoke with `/skill-name` in Claude Code, or by natural language description.

| Skill                  | Command               | What it does                                                                              |
|------------------------|------------------------|------------------------------------------------------------------------------------------|
| **code-review**        | `/code-review`         | Checklist-based review of `git diff` — style, logging, exceptions, API, security         |
| **commit**             | `/commit`              | Git Flow-aware commit: auto-detects branch, splits logical units, enforces message format |
| **test**               | `/test`                | Runs tests (full or module), summarizes failures, offers auto-fix via test-fixer          |
| **write-pr**           | `/write-pr`            | Analyzes commits since base, generates PR title/body/labels, creates via gh CLI           |
| **security-checklist** | `/security-checklist`  | Grep-based scan: hardcoded secrets, SQL injection, missing auth, sensitive logs           |
| **migration-guide**    | `/migration-guide`     | DB schema change guide: Entity→DTO→Repo→Service→Test order + 2-phase deletion            |

## Agents (Auto-triggered)

Agents activate based on natural language triggers. See `.claude/agents/` for full specs.

| Agent | Trigger phrases | Role |
|-------|-----------------|------|
| **convention-validator** | "컨벤션 검사해줘", "convention-validator 실행해" | Detects + auto-fixes convention violations |
| **test-fixer** | "테스트 고쳐줘", "test-fixer 실행해" | Diagnoses and fixes failing tests (up to 3 retries) |
| **contradiction-finder** | "모순 찾아줘", "일관성 검사해줘" | Read-only audit: docs↔code↔agents consistency |
| **doc-polisher** | "문서 갱신해줘", "문서 정리해줘" | Fixes outdated snippets, undocumented patterns, heading errors |
| **prompt-polisher** | "프롬프트 다듬어줘" | Read-only: Before/After suggestions for agent/skill quality |
| **web-researcher** | "최신 정보 조사해줘", "CVE 확인해줘" | Web search for post-training-cutoff info (uses haiku model) |

## Rules Files

Full rule specifications live in `.claude/rules/`:

- `coding-style.md` — Annotations, Lombok, naming, import order
- `logging.md` — SLF4J levels, parameterized logging, forbidden patterns
- `exception-handling.md` — `ExpectedException` usage, HTTP status mapping
- `testing.md` — JUnit 5, Mockito BDD style, Describe/Context/It structure
- `commit-convention.md` — type/scope format, branch strategy
- `api-convention.md` — URL structure, response wrapper, Swagger annotations

## Development Commands

```bash
# Run application
./gradlew bootRun

# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "team.themoment.hellogsmv3.domain.member.service.CreateMemberServiceTest"

# Format code
./gradlew spotlessApply

# Build
./gradlew build
```

## Branch Strategy

| Branch                  | Purpose                              |
|-------------------------|--------------------------------------|
| `main`                  | Production releases                  |
| `develop`               | Integration branch — PRs target here |
| `feature/{type}/{desc}` | Feature work                         |
| `hotfix/{desc}`         | Hotfix — targets main                |