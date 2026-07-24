# CLAUDE.md — hellogsm-monorepo

> Hello, GSM | 광주소프트웨어마이스터고등학교 입학 지원 서비스

## Project Overview

Gradle multi-module monorepo. The core is a Spring Boot 4 / Java 25 REST API server for GSM school admissions; it now also hosts the `entrance` engine (Kotlin), absorbed via `git subtree` from the former `every-entrance` repo to declaratively model yearly admission-plan rules (scoring, evaluation, major assignment).

| Item      | Value                                    |
|-----------|------------------------------------------|
| Language  | Java 25 (`server`, `persistence`) + Kotlin 2.3.21 (`entrance-*`) |
| Framework | Spring Boot 4.0.5                        |
| Build     | Gradle (multi-module, JVM target 25 across all modules) |
| ORM       | JPA + QueryDSL 7.1                       |
| DB        | MySQL + Redis                            |
| Auth      | Spring Security + OAuth2 (Google, Kakao) |
| Testing   | JUnit 5 + Mockito 5 (server) / kotlin.test + JUnit5 (entrance) |

## Module Structure

| Module            | Role                                                                   | Depends on |
|-------------------|-------------------------------------------------------------------------|------------|
| `server`          | Spring Boot API — controllers, services, security, batch triggers       | `persistence` |
| `persistence`     | Shared JPA entities/repositories (`Oneseo`, `MiddleSchoolAchievement`, …), reused by `server` and `entrance-batch` | — |
| `entrance-dsl`    | Kotlin type-safe builder + immutable domain model (`AdmissionPlan`) for admission-plan rules | none (pure Kotlin) |
| `entrance-plans`  | Year-by-year plan declarations (`Plan2026.kt`) — data only, no logic     | `entrance-dsl` |
| `entrance-engine` | Pure-function interpreter: `scoring` → `evaluation` → `assignment`      | `entrance-dsl` |
| `entrance-batch`  | DB-backed batch runner invoking `entrance-engine` (replaces the old `go-hellogsm`) | `entrance-engine`, `persistence` |
| `entrance-lambda` | Mock score-calculation API, deployed standalone to AWS Lambda (replaces the old `go-hellogsm-score-calculator`) — no Spring, plain `RequestHandler` to minimize cold start | `entrance-engine`, `entrance-plans` |

**Hard rule carried over from the migration:** `entrance-dsl`/`entrance-plans`/`entrance-engine` never declare `server` or `persistence` as a dependency — this keeps "the engine doesn't know about the DB" enforced at compile time. Only `entrance-batch` is allowed to depend on `persistence`; `entrance-lambda` never depends on `server` or `persistence` either.

For anything inside `entrance/`, read [`entrance/CLAUDE.md`](./entrance/CLAUDE.md) first — its conventions (Kotlin DSL design principles, `BigDecimal`-only scoring, Korean-backtick `kotlin.test` naming, plan-file-per-year policy) are **module-specific and differ from** the Java/Spring rules below. Background and roadmap: [`entrance/CONTEXT.md`](./entrance/CONTEXT.md), [`entrance/PLAN.md`](./entrance/PLAN.md). Architecture docs: [`docs/entrance/`](./docs/entrance/README.md).

## Rules Files

Full rule specifications live in `.claude/rules/` — read the files there for detailed rules on coding style, logging, exception handling, testing, commit conventions, and API conventions. **These rules apply to the Java/Spring modules (`server`, `persistence`) only** — Kotlin `entrance-*` modules follow [`entrance/CLAUDE.md`](./entrance/CLAUDE.md) instead.

## Development Commands

```bash
# Run the API server
./gradlew :server:bootRun

# Run all tests (every module)
./gradlew test

# Run only the server's tests
./gradlew :server:test

# Run a specific server test class
./gradlew :server:test --tests "team.themoment.hellogsmv3.domain.member.service.CreateMemberServiceTest"

# Run entrance engine tests (see entrance/CLAUDE.md for module-scoped commands)
./gradlew :entrance-engine:test

# Format code
./gradlew spotlessApply

# Build everything
./gradlew build
```

## Branch Strategy

| Branch                  | Purpose                              |
|-------------------------|--------------------------------------|
| `main`                  | Production releases                  |
| `develop`               | Integration branch — PRs target here |
| `feature/{type}/{desc}` | Feature work                         |
| `hotfix/{desc}`         | Hotfix — targets main                |
