# CLAUDE.md — every-entrance

hellogsm(광주소프트웨어마이스터고 입학지원시스템)의 입학전형 요강을 Kotlin DSL로 선언하고, 이를 해석하는 엔진으로 성적 계산·전형 배치를 수행하는 라이브러리. 배경은 [CONTEXT.md](./CONTEXT.md), 스펙과 로드맵은 [PLAN.md](./PLAN.md), DSL 사용법은 [README.md](./README.md) 참고.

## 모듈 구조

| 모듈 | 역할 | 의존성 |
|---|---|---|
| `entrance-dsl` | 도메인 모델(`plan/`) + type-safe builder(`dsl/`) | 없음 (순수 Kotlin) |
| `entrance-plans` | 연도별 요강 선언 (`Plan2026.kt`) — **데이터만, 로직 금지** | `entrance-dsl` |
| `entrance-engine` | 해석 엔진 (scoring → evaluation → assignment 순으로 구현 예정) | `entrance-dsl` (+test: `entrance-plans`) |

패키지 루트: `kr.hellogsm.entrance`

## 빌드 / 테스트

```bash
./gradlew build          # 전체 빌드 + 테스트
./gradlew test           # 테스트만
./gradlew :entrance-dsl:test --tests '*PlanValidatorTest*'   # 단건
```

Kotlin 2.3.21, Gradle wrapper 9.6.1, **JVM target 21 고정** (server Java 25·AWS Lambda 겸용 — 올리지 말 것).

## 핵심 설계 원칙 (위반 금지)

1. **DSL은 데이터, 엔진은 로직.** `admissionPlan { }`의 산출물은 불변 `AdmissionPlan` 모델이다. 모델에 람다/함수 타입 필드를 넣지 않는다 — 규칙이 표현 안 되면 모델을 확장하고, 위원회 재량 조항은 엔진의 수동 오버라이드 입력으로 처리한다.
2. **모든 plan은 생성 시점에 검증된다.** `AdmissionPlan.init` → `PlanValidator`. 새 모델 필드를 추가하면 반드시 검증 규칙도 함께 추가한다. 검증 오류는 모아서 `PlanValidationException(errors)`로 한 번에 던진다.
3. **점수는 전부 `BigDecimal`.** Double 연산 금지. 반올림은 plan의 `RoundingPolicy`(중간값 scale 5 / 결과값 scale 3, HALF_UP)를 따른다. DSL 빌더에서 Double을 받을 땐 `BigDecimal.valueOf()` 사용 (`Double.toBigDecimal()`은 이진 오차를 그대로 가져오므로 금지).
4. **연도별 plan 파일은 수정이 아니라 추가.** 새 학년도 = `PlanXXXX.kt` 신규 파일. 과거 plan은 재현성을 위해 보존한다.
5. **plan 수치 변경은 근거 문서와 함께.** `entrance-plans`의 수치는 요강 PDF가 유일한 근거다. `Plan2026Test`는 요강 수치를 고정하는 테스트이므로, 요강 개정 없이 이 테스트를 고쳐서 통과시키지 않는다.

## 컨벤션

- 오류 메시지·테스트 이름·KDoc은 한국어. 도메인 용어는 요강 표기를 따른다 (예: 전형, 차수, 정원 외, 동점자 처리).
- 테스트는 `kotlin.test` + JUnit5 (`useJUnitPlatform`). 백틱 한국어 테스트명 사용.
- DSL 빌더는 `@AdmissionDsl`(`@DslMarker`) 적용. 중첩 블록에서 공유해야 하는 헬퍼는 `ScoreComponentScope`처럼 공통 상위 클래스로 추출한다 (DslMarker가 외부 리시버 접근을 막기 때문).
- 빌더의 필수 값은 nullable var + `requireNotNull(...)` (build 시점 실패), 모델 정합성은 `PlanValidator` (생성 시점 실패)로 역할을 나눈다.

## 현재 상태 / 주의사항

- **Phase 0 완료** (DSL + Plan2026 + 검증). 다음은 Phase 1: `entrance-engine/scoring` — [PLAN.md](./PLAN.md) 8절 로드맵 참고.
- ⚠️ **검정고시 봉사 환산식 미확정**: `Plan2026.kt`의 `formula(GED) { volunteer(...) }`는 교과와 동형이라는 *가정*이며 TODO 주석이 붙어 있다. `go-hellogsm-score-calculator` 구현과 대조 전까지 확정값으로 취급하지 말 것.
- 참고 자료: `.reference/CLAUDE.md` (hellogsm 프로덕트 전체 개요), `.reference/2026_entrance.pdf` (2026 요강 원문 — plan 수치의 근거 문서).
- 기존 Go 구현과의 parity 검증(golden test)이 Phase 1~2의 완료 기준이다. 요강과 기존 구현이 다르면 **요강이 정답**이며, 차이는 문서화한다.
