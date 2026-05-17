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

## Rules Files

Full rule specifications live in `.claude/rules/` — read the files there for detailed rules on coding style, logging, exception handling, testing, commit conventions, and API conventions.

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

| Branch            | Purpose                                                 |
|-------------------|---------------------------------------------------------|
| `main`            | Production releases                                     |
| `develop`         | Integration branch — PRs target here                    |
| `{type}/{desc}`   | Work branch — single type token (cut from `develop`)    |
| `hotfix/{desc}`   | Hotfix — cut from and targets `main`                    |

`{type}`은 commit type 어휘와 동일하게 단일 토큰만 허용: `feature`, `fix`, `update`, `refactor`, `add`, `chore`, `docs`, `test`, `code`, `ci/cd`.

- ✅ `update/write-pr-convention`, `fix/null-achievement-validation`, `refactor/approved-oneseo-temp-storage`
- ❌ `feature/update/write-pr-convention`, `feature/fix/null-achievement-validation` — 이중 type 토큰 금지
