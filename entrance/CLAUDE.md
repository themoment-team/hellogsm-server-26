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

- **Phase 0 완료** (DSL + Plan2026 + 검증). **Phase 1 완료** (2026-07-20): `entrance-engine/scoring` + Go 대비 golden test. **Phase 2 — 엔진 전 범위 완료** (2026-07-21): `evaluation`(1차/2차 선발·편입·동점자·추가모집) + `assignment`(학과 배정·예비합격·중도포기 재배정) + go-hellogsm 대비 golden test. 남은 것: `entrance-batch`(DB 러너), 실 배치 대비 재검증(Go 툴체인 필요), `entrance-lambda` 배포 — [PLAN.md](./PLAN.md) 8절 로드맵 참고.
- **저장소 통합 결정 (2026-07-20)**: 이 레포는 최종적으로 `hellogsm-server-26`에 **멀티모듈로 흡수**된다. 방향은 `every-entrance` → 서버 레포(`git subtree`, 히스토리 보존)이며, 반대 방향(서버를 새 레포로 이전)은 CI/CD·CodeDeploy·1,882커밋 히스토리 이전 비용 때문에 기각됐다. 통합 후에도 `entrance-engine`은 서버·persistence 모듈을 **의존성으로 선언하지 않는다** — 모듈 그래프가 "엔진은 DB를 모른다"를 컴파일 타임에 강제한다. 상세 계획은 [MIGRATION.md](./MIGRATION.md).
- **`entrance-lambda` 유지 결정 (2026-07-20)**: 원래 MVP 범위는 아니었으나 이미 착수했고, 모의 성적 계산의 가용성 요건(server 다운 시에도 동작)도 유효하므로 별도 배포 아티팩트로 유지한다. JVM target 21 고정의 근거이기도 하다.
- **최종 동점자에 3-2 학기 미사용 확정 (2026-07-20)**: 졸업자도 3-2를 동점자 기준에 넣지 않는다 (요강 명시 범위 = 3-1·2-2·2-1·1-2). `Plan2026.kt`가 이미 그 상태이고 `Tiebreakers`는 plan에 선언된 학기만 읽으므로 코드 변경은 없다 — 향후 plan에서도 3-2를 추가하지 않는다.
- ⚠️ **요강 vs Go 확인된 산출 차이**: 학기 몫 scale-5 중간 반올림(표본 ~13%에서 ±0.001), 검정고시 평균의 float64 이진 오차 — 엔진은 요강을 따른다. 상세는 [PLAN.md](./PLAN.md) 7절 2항.
- **검정고시 환산식 확정 (2026-07-20)**: 교과 = (평균−50)÷50×240, 봉사 = (평균−40)÷60×30, 음수는 0 처리. 요강 PDF p.26 수식 원문(글리프 해독)과 2026 시즌 Go 코드가 일치함을 확인.
- ⚠️ **Go 계산기 HEAD는 2027 시즌 코드**: `go-hellogsm-score-calculator`는 2026-07-13 커밋(4e8d668)부터 졸업자 배점이 18/36/36/45/45로 변경됨(2027학년도 대비 추정). **Plan2026 parity 기준은 4e8d668 이전 코드**(졸업자 36/36/54/54 — 2026 요강과 일치)다.
- ⚠️ **go-hellogsm HEAD도 2027 시즌으로 추정되는 변경이 반영됨**: 2026-07-17 커밋(da09df4)부터 정원이 72→64명(SW 36→32, IOT/AI 18→16 등)으로 변경됨. **evaluation·assignment parity 기준은 da09df4 이전 코드**(72명 — 2026 요강·Plan2026과 일치)다. 최종 동점자 순서 버그 수정(f4b17bc, 2026-07-15)은 2026 시즌 실제 원서접수(2025-10)·평가(2025-11)보다 한참 뒤에 머지됐다 — 그 해 실제 배치는 버그(역순) 상태로 돌았을 가능성이 있다. 원칙(요강이 정답)에 따라 엔진은 수정된(spec) 순서를 따른다.
- ⚠️ **중도포기 재배정의 정원 외 빈자리**: go-hellogsm은 정원 내 포기자만큼 정원 외 자리도 열어 학과당 2명 상한을 넘길 수 있다(버그로 판단). 엔진은 정원 내·정원 외 풀의 빈자리를 독립적으로 계산한다 — [PLAN.md](./PLAN.md) 7절 2항.
- **추가모집(`RE_EVALUATE`)은 parity 검증 대상이 아님**: go-hellogsm에 대응 배치가 없어 요강 8-바만을 근거로 구현했다.
- 참고 자료: `.reference/CLAUDE.md` (hellogsm 프로덕트 전체 개요), `.reference/2026_entrance.pdf` (2026 요강 원문 — plan 수치의 근거 문서).
- 기존 Go 구현과의 parity 검증(golden test)이 Phase 1~2의 완료 기준이다. 요강과 기존 구현이 다르면 **요강이 정답**이며, 차이는 문서화한다.
