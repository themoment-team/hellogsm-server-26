# Entrance Module Rules — Kotlin DSL / Engine

> Scope: Kotlin `entrance-*` modules (`entrance-dsl`, `entrance-plans`, `entrance-engine`, `entrance-batch`, `entrance-lambda`) only. Java/Spring modules (`server`, `persistence`) follow the other files in this directory instead. Module roles/dependencies are documented in the root `CLAUDE.md` module table — this file covers Kotlin-specific coding conventions only.

## Package root

`kr.hellogsm.entrance`

## Build / Test

```bash
./gradlew build                                             # 전체 빌드 + 테스트 (엔진 모듈)
./gradlew test                                              # 테스트만
./gradlew :entrance-dsl:test --tests '*PlanValidatorTest*'  # 단건 테스트
```

## Core Design Principles (do not violate)

1. **DSL is data, the engine is logic.** `admissionPlan { }` produces an immutable `AdmissionPlan` model. Never add lambda/function-typed fields to the model — if a rule can't be expressed, extend the model instead. Committee-discretion clauses are handled as manual override inputs to the engine, never encoded as rules.
2. **Every plan is validated at construction time.** `AdmissionPlan.init` → `PlanValidator`. Adding a new model field requires adding its validation rule in the same change. Validation errors are collected and thrown together as a single `PlanValidationException(errors)`.
3. **Scores are always `BigDecimal`.** No `Double` arithmetic. Rounding follows the plan's `RoundingPolicy` (intermediate scale 5 / result scale 3, HALF_UP). When a DSL builder accepts a `Double`, use `BigDecimal.valueOf()` — never `Double.toBigDecimal()` (it carries binary floating-point error through).
4. **The active plan only ever exists under the fixed name `Plan.kt` / `val plan`.** Consumers (`entrance-batch`, `entrance-lambda`) reference `kr.hellogsm.entrance.plans.plan` without knowing the year. When moving to a new admission year, don't edit `Plan.kt` in place — archive the current content to `legacy/PlanXXXX.kt` (rename symbol to `planXXXX`) first, then overwrite `Plan.kt`. Full procedure: `entrance/README.md` § "새 학년도 요강 추가하기".
5. **Plan values must cite their source document.** The admission-plan PDF is the only basis for numbers in `entrance-plans`. `PlanTest` pins the active plan's published figures — never loosen this test to make it pass without an actual plan revision backing the change.

## Conventions

- Error messages, test names, and KDoc are in Korean. Domain terms follow the official admission-plan wording (전형, 차수, 정원 외, 동점자 처리, etc.).
- Tests use `kotlin.test` + JUnit5 (`useJUnitPlatform`), with backtick Korean test names.
- DSL builders are annotated `@AdmissionDsl` (`@DslMarker`). Helpers shared across nested blocks are extracted into a common superclass (e.g. `ScoreComponentScope`) since `@DslMarker` blocks outer-receiver access.
- Builder required values use a nullable `var` + `requireNotNull(...)` (fails at `build()` time). Cross-field model consistency is `PlanValidator`'s job (fails at construction time). Keep these two failure modes separate.
