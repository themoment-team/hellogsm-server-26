# CLAUDE.md — every-entrance

hellogsm(광주소프트웨어마이스터고 입학지원시스템)의 입학전형 요강을 Kotlin DSL로 선언하고, 이를 해석하는 엔진으로 성적 계산·전형 배치를 수행하는 라이브러리. 배경은 [CONTEXT.md](./CONTEXT.md), 스펙과 로드맵은 [PLAN.md](./PLAN.md), DSL 사용법은 [README.md](./README.md) 참고.

## 모듈 구조

| 모듈 | 역할 | 의존성 |
|---|---|---|
| `entrance-dsl` | 도메인 모델(`plan/`) + type-safe builder(`dsl/`) | 없음 (순수 Kotlin) |
| `entrance-plans` | 현재 활성 요강 선언(`Plan.kt`, 고정 이름) + 지난 연도 보관(`legacy/`) — **데이터만, 로직 금지** | `entrance-dsl` |
| `entrance-engine` | 해석 엔진 (scoring → evaluation → assignment 순으로 구현 예정) | `entrance-dsl` (+test: `entrance-plans`) |

패키지 루트: `kr.hellogsm.entrance`

## 빌드 / 테스트

```bash
./gradlew build          # 전체 빌드 + 테스트
./gradlew test           # 테스트만
./gradlew :entrance-dsl:test --tests '*PlanValidatorTest*'   # 단건
```

Kotlin 2.3.21, Gradle wrapper 9.6.1, **JVM target 25** (server·엔진·`entrance-lambda` 모두 25로 통일 — 2026-07-21, AWS Lambda의 Java 25 매니지드 런타임 지원 확인으로 기존 21 고정 해제). 빌드는 JDK 25 하나면 된다.

## 핵심 설계 원칙 (위반 금지)

1. **DSL은 데이터, 엔진은 로직.** `admissionPlan { }`의 산출물은 불변 `AdmissionPlan` 모델이다. 모델에 람다/함수 타입 필드를 넣지 않는다 — 규칙이 표현 안 되면 모델을 확장하고, 위원회 재량 조항은 엔진의 수동 오버라이드 입력으로 처리한다.
2. **모든 plan은 생성 시점에 검증된다.** `AdmissionPlan.init` → `PlanValidator`. 새 모델 필드를 추가하면 반드시 검증 규칙도 함께 추가한다. 검증 오류는 모아서 `PlanValidationException(errors)`로 한 번에 던진다.
3. **점수는 전부 `BigDecimal`.** Double 연산 금지. 반올림은 plan의 `RoundingPolicy`(중간값 scale 5 / 결과값 scale 3, HALF_UP)를 따른다. DSL 빌더에서 Double을 받을 땐 `BigDecimal.valueOf()` 사용 (`Double.toBigDecimal()`은 이진 오차를 그대로 가져오므로 금지).
4. **현재 plan은 `Plan.kt`라는 고정 이름/심볼(`val plan`)로만 존재한다.** 소비자(`entrance-batch`, `entrance-lambda`)는 연도를 몰라도 `kr.hellogsm.entrance.plans.plan`만 참조하면 된다 (2026-07-28 결정 — 이전엔 `Plan2026.kt`/`plan2026`처럼 연도가 심볼명에 박혀 있어 서버 등 신규 소비자가 매년 이름을 갱신해야 했다). 새 학년도로 넘어갈 때는 `Plan.kt`를 고치는 게 아니라 ① 지금 내용을 `legacy/PlanXXXX.kt`로 옮기고 심볼명을 `planXXXX`로 바꿔 얼린 뒤(과거 plan은 재현성을 위해 보존) ② `Plan.kt`를 새 연도 내용으로 덮어쓴다. 절차는 [`entrance-plans/.../plans/legacy/README.md`](./entrance-plans/src/main/kotlin/kr/hellogsm/entrance/plans/legacy/README.md) 참고.
5. **plan 수치 변경은 근거 문서와 함께.** `entrance-plans`의 수치는 요강 PDF가 유일한 근거다. `PlanTest`는 현재 활성 plan의 요강 수치를 고정하는 테스트이므로, 요강 개정 없이 이 테스트를 고쳐서 통과시키지 않는다.

## 컨벤션

- 오류 메시지·테스트 이름·KDoc은 한국어. 도메인 용어는 요강 표기를 따른다 (예: 전형, 차수, 정원 외, 동점자 처리).
- 테스트는 `kotlin.test` + JUnit5 (`useJUnitPlatform`). 백틱 한국어 테스트명 사용.
- DSL 빌더는 `@AdmissionDsl`(`@DslMarker`) 적용. 중첩 블록에서 공유해야 하는 헬퍼는 `ScoreComponentScope`처럼 공통 상위 클래스로 추출한다 (DslMarker가 외부 리시버 접근을 막기 때문).
- 빌더의 필수 값은 nullable var + `requireNotNull(...)` (build 시점 실패), 모델 정합성은 `PlanValidator` (생성 시점 실패)로 역할을 나눈다.

## 현재 상태 / 주의사항

- **`Plan.kt` 고정 네이밍 결정 (2026-07-28)**: `entrance-plans`의 활성 plan 파일/심볼을 연도 붙은 이름(`Plan2026.kt`/`plan2026`)에서 `Plan.kt`/`val plan`으로 바꿨다. 연도가 심볼명에 박혀 있으면 새 소비자(예: server가 학과/전형 코드 유효성 검사에 plan을 재사용하는 안)를 추가할 때마다 참조를 갱신해야 하는데, 활성 plan을 가리키는 이름을 고정하면 그 문제가 사라진다. 지난 연도 plan은 `entrance-plans/.../plans/legacy/`로 옮기고 심볼명을 `planXXXX`로 바꿔 보존한다 — 절차는 `legacy/README.md` 참고. 별도 환경변수·설정 파일 없이 "어떤 plan이 활성인지"를 import 경로 하나로 해결하는 방식을 택했다(활성 plan 선택 메커니즘 자체가 이전엔 없었음 — CONTEXT.md 미해결 질문 참고).
- **Phase 0 완료** (DSL + Plan2026 + 검증). **Phase 1 완료** (2026-07-24): `entrance-engine/scoring` + Go 대비 golden test, `entrance-lambda`(모의 성적 계산 API) 구현·단위 테스트·CI/CD 완료. **Phase 2 — 엔진 전 범위 완료** (2026-07-21): `evaluation`(1차/2차 선발·편입·동점자·추가모집) + `assignment`(학과 배정·예비합격·중도포기 재배정) + go-hellogsm 대비 golden test, `entrance-batch`(DB 러너) 완료. 남은 것: 실 배치 대비 재검증(Go 툴체인 필요), `entrance-lambda`의 AWS 실배포(함수·API Gateway 생성은 인프라 작업이라 코드 밖) — [PLAN.md](./PLAN.md) 8절 로드맵 참고.
- **저장소 통합 결정 (2026-07-20)**: 이 레포는 최종적으로 `hellogsm-server-26`에 **멀티모듈로 흡수**된다. 방향은 `every-entrance` → 서버 레포(`git subtree`, 히스토리 보존)이며, 반대 방향(서버를 새 레포로 이전)은 CI/CD·CodeDeploy·1,882커밋 히스토리 이전 비용 때문에 기각됐다. 통합 후에도 `entrance-engine`은 서버·persistence 모듈을 **의존성으로 선언하지 않는다** — 모듈 그래프가 "엔진은 DB를 모른다"를 컴파일 타임에 강제한다. 상세 계획은 [MIGRATION.md](./MIGRATION.md).
- **`entrance-lambda` 유지 결정 (2026-07-20)**: 원래 MVP 범위는 아니었으나 이미 착수했고, 모의 성적 계산의 가용성 요건(server 다운 시에도 동작)도 유효하므로 별도 배포 아티팩트로 유지한다. (2026-07-21 업데이트: AWS Lambda가 Java 25 매니지드 런타임을 지원함을 확인해 엔진·Lambda JVM target을 21→25로 통일했다 — 더 이상 21 고정 근거가 아니다.)
- **`entrance-lambda` 구현 완료 (2026-07-24)**: Spring 없이 `aws-lambda-java-core` `RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>`로 직접 구현(콜드 스타트 최소화, 프레임워크 부트스트랩 비용 없음) — **API Gateway REST API(v1 프록시 통합) 계약을 전제**로 하므로 Lambda Function URL이나 HTTP API(v2 payload)로 앞단을 구성하면 이벤트 스키마가 달라 동작하지 않는다. `go-hellogsm-score-calculator`와 요청/응답 JSON·인증 헤더(`x-hg-api-key`)가 완전히 동일해 **server 쪽 전환은 `SCORE_CALCULATOR_SERVICE_URL` 환경변수 교체만으로 가능**하다(코드 변경 불필요) — 단 `achievement1_1`은 결측 학기 대체(`MissingSemesterStrategy`)가 항상 최종 채점 시점(`ScoringEngine`)에 plan 선언대로 적용되어야 한다는 원칙(2026-07-24)에 따라 그대로 submitted map에 넣어 엔진에 맡긴다 — plan이 1-1 자체를 채점하지는 않지만 `SAME_YEAR_OTHER_SEMESTER` 전략의 대체 원본으로는 쓰인다. **`entrance-batch`도 이 원칙을 지키도록 정리 완료(2026-07-24)**: `MiddleSchoolAchievement` 엔티티에 `achievement_1_1` 컬럼을 추가하고, server의 `OneseoService.buildCalcDto`(구 `buildCalcDtoWithFillEmpty`)에서 접수 시점 결측 학기 대체(1-1→1-2 등 하드코딩된 규칙)를 전부 제거했다 — 이제 원본 검증만 하고 그대로 저장하며, 대체는 배치·람다 양쪽 모두 최종 채점 시점에 엔진이 담당한다. **DB 스키마 변경(운영 반영은 별도 작업)**: `ALTER TABLE tb_middle_school_achievement ADD COLUMN achievement_1_1 VARCHAR(255) NULL;` — `ddl-auto`가 `validate`/`none`인 환경에는 수동 적용 필요(`.agents/skills/migration-guide` 참고). AWS 실배포(함수 생성·IAM·API Gateway 연결)는 기존 Go 함수와 병행 배포로 결정(2026-07-24) — 함수 자체는 콘솔/IaC로 최초 1회 준비해야 하고 CI/CD(`.github/workflows/entrance-lambda-*.yml`)는 코드 갱신만 담당한다.
- **최종 동점자에 3-2 학기 미사용 확정 (2026-07-20)**: 졸업자도 3-2를 동점자 기준에 넣지 않는다 (요강 명시 범위 = 3-1·2-2·2-1·1-2). `Plan.kt`(구 `Plan2026.kt`)가 이미 그 상태이고 `Tiebreakers`는 plan에 선언된 학기만 읽으므로 코드 변경은 없다 — 향후 plan에서도 3-2를 추가하지 않는다.
- ⚠️ **요강 vs Go 확인된 산출 차이**: 학기 몫 scale-5 중간 반올림(표본 ~13%에서 ±0.001), 검정고시 평균의 float64 이진 오차 — 엔진은 요강을 따른다. 상세는 [PLAN.md](./PLAN.md) 7절 2항.
- **검정고시 환산식 확정 (2026-07-20)**: 교과 = (평균−50)÷50×240, 봉사 = (평균−40)÷60×30, 음수는 0 처리. 요강 PDF p.26 수식 원문(글리프 해독)과 2026 시즌 Go 코드가 일치함을 확인.
- ⚠️ **Go 계산기 HEAD는 2027 시즌 코드**: `go-hellogsm-score-calculator`는 2026-07-13 커밋(4e8d668)부터 졸업자 배점이 18/36/36/45/45로 변경됨(2027학년도 대비 추정). **현재 plan(2026) parity 기준은 4e8d668 이전 코드**(졸업자 36/36/54/54 — 2026 요강과 일치)다.
- ⚠️ **go-hellogsm HEAD도 2027 시즌으로 추정되는 변경이 반영됨**: 2026-07-17 커밋(da09df4)부터 정원이 72→64명(SW 36→32, IOT/AI 18→16 등)으로 변경됨. **evaluation·assignment parity 기준은 da09df4 이전 코드**(72명 — 2026 요강·현재 plan과 일치)다. 최종 동점자 순서 버그 수정(f4b17bc, 2026-07-15)은 2026 시즌 실제 원서접수(2025-10)·평가(2025-11)보다 한참 뒤에 머지됐다 — 그 해 실제 배치는 버그(역순) 상태로 돌았을 가능성이 있다. 원칙(요강이 정답)에 따라 엔진은 수정된(spec) 순서를 따른다.
- ⚠️ **중도포기 재배정의 정원 외 빈자리**: go-hellogsm은 정원 내 포기자만큼 정원 외 자리도 열어 학과당 2명 상한을 넘길 수 있다(버그로 판단). 엔진은 정원 내·정원 외 풀의 빈자리를 독립적으로 계산한다 — [PLAN.md](./PLAN.md) 7절 2항.
- **추가모집(`RE_EVALUATE`)은 parity 검증 대상이 아님**: go-hellogsm에 대응 배치가 없어 요강 8-바만을 근거로 구현했다.
- 참고 자료: `.reference/CLAUDE.md` (hellogsm 프로덕트 전체 개요), `.reference/2026_entrance.pdf` (2026 요강 원문 — plan 수치의 근거 문서).
- 기존 Go 구현과의 parity 검증(golden test)이 Phase 1~2의 완료 기준이다. 요강과 기존 구현이 다르면 **요강이 정답**이며, 차이는 문서화한다.
