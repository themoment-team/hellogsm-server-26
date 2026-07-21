# CONTEXT.md — every-entrance 프로젝트 배경

> 이 문서는 프로젝트의 배경과 진행 맥락을 요약한다. 구현 계획·스펙은 [PLAN.md](./PLAN.md), 개발 규칙은 [CLAUDE.md](./CLAUDE.md), DSL 사용법은 [README.md](./README.md) 참고.

## 프로덕트 배경

hellogsm(www.hellogsm.kr)은 광주소프트웨어마이스터고등학교 입학지원시스템이다. 현재 5개 저장소로 구성되어 있다 (상세: `.reference/CLAUDE.md`):

- `hellogsm-server-26` — 메인 API 서버 (Spring Boot 4, Java 25)
- `hellogsm-client-26` — 프론트엔드 모노레포 (지원자용 client / 운영자용 admin)
- `go-hellogsm` — 입학 전형 배치 잡 (1차/2차 평가, 최종 학과 배정) (Go)
- `go-hellogsm-score-calculator` — 내신 성적 계산 엔진 (Go, AWS Lambda)
- `go-hellogsm-ops` — 운영 보조 도구 (Go)

## 문제 인식

- 해마다 새 입학 전형 요강이 나오면, 변경된 요구사항을 코드 곳곳에서 직접 찾아 수정해야 했다. 전형 로직이 server(Java) / 배치(Go) / 성적계산 Lambda(Go)에 흩어져 있어 수정 범위가 넓고 누락 위험이 크다.
- 그러나 여러 해를 거치며 입학요강이 몇 가지 형태로 **고착화**되는 패턴이 관찰됐다. 즉 "매년 바뀌는 것"은 임의의 코드가 아니라 **정형화 가능한 파라미터와 규칙의 조합**이다.

## 제안 (이번 작업의 목표)

전형 요강을 **Kotlin DSL**로 선언하고, 이를 해석하는 공용 엔진이 실제 로직을 수행하는 구조로 개편한다. 연도별 요강 변경 = DSL 파일 수정으로 수렴시키는 것이 최종 지향점.

- **최종 비전**: UI부터 Server까지 DSL 하나로 관리
- **1차 MVP 범위**: **Server Logic만** DSL로 관리 (성적 계산 + 전형 배치). UI 생성은 이후 단계.
- 기존에 Go/Lambda로 분리돼 있던 서버 로직도 이번 작업에서 모두 Kotlin으로 재작성해 통합한다.

## DSL로 일반화할 도메인

사용자가 직접 식별한 도메인:

1. 학과의 종류, 학과별 정원
2. 전형의 종류 — 일반 / 특별(사회통합) / 국가보훈 등. **정원 내 / 정원 외 구분 필수**
3. 전형 절차 — N차 전형 유동적 관리, 선발인원 배율(예: 1.3배수), 점수 산출 기준
4. 학생 성적 — 졸업예정자/졸업자/검정고시 구분에 따라 학기별 반영 비율이 달라지고, 각 학기는 일반교과/비교과(+예체능) 등으로 나뉘어 산출됨
5. 별도의 점수 시스템 — 면접 점수, 역량검사 점수 등 수동으로 숫자 입력되는 점수

2026 요강 PDF(`.reference/2026_entrance.pdf`) 검토로 추가 식별된 도메인 — **놓치면 배치 결과가 틀어지는 규칙들**:

6. **동점자 처리 기준** — 1차와 최종(2차)이 서로 다른 비교자(comparator) 체인을 가짐
7. **전형 간 이동(fallback) 규칙** — 특별전형 미충원분 → 일반전형 추가 선발, 특별전형 탈락자 → 일반전형에 포함 전형, 정원외 전형은 "1차 합격자 최저점 이내"일 때만 정원외로 전형하고 초과 시 사회통합전형으로 편입
8. **학과 배정 규칙** — 3지망까지 필수 기재, 성적순 지망 배정, 정원외 합격자는 **학과당 최대 2명** 상한, 정원 미달 시 지원과 변경 배정 가능
9. **예비 합격자** — 모집정원의 3% 범위, 일반전형 불합격자 중 고득점 순
10. **추가모집** — 정원 미달 시에만, 일반전형으로만, **1차 환산점수만으로** 선발 (기존 배치의 `RE_EVALUATE`)
11. **수치 정밀도 정책** — 중간값은 소수점 여섯째 자리에서 반올림해 다섯째 자리까지, 결과값은 소수점 넷째 자리에서 반올림 등 요강에 명시된 자릿수 규칙
12. **결측 성적 대체 규칙** — 학기 성적이 없으면 같은 학년 다른 학기 → 차상위 학년 → 차하위 학년 순으로 대체. 비교과가 없는 학년은 출석 5점/봉사 2점 기본점 부여
13. **출석/봉사 환산 계단함수** — 미인정 지각·조퇴·결과 3회 = 결석 1일(버림), 결석 1일당 −3점, 10일 이상 0점 / 봉사 연 7시간 이상 10점 ~ 3시간 이하 2점
14. **불참(미응시) 처리** — 전형 대상에서 제외
15. **총원제(All-cut)** — 1·2차 전형은 학과 구분 없이 총원 기준 선발
16. **전형 일정** — 접수·발표·검사 기간 (MVP에서는 상태 전이 참고용, UI 단계에서 본격 활용)
17. 지원 자격 / 사회통합전형 세부 유형(기초생활수급자 등 5종) / 제출 서류 체크리스트 — 서버 로직 밖 요소가 많아 MVP에서는 열거형 수준으로만 모델링

또한 요강 내 "2028학년도 행정예고"(학교자율시간 과목 제외 등)는 **규칙이 실제로 계속 변한다는 근거**로, 이번 개편의 타당성을 뒷받침한다.

## 지금까지의 결정 사항

- 구현 언어/방식: **Kotlin DSL** (type-safe builder)
- Go로 분리된 배치(`go-hellogsm`)와 성적계산 Lambda(`go-hellogsm-score-calculator`) 로직을 Kotlin으로 재작성해 이 개편에 흡수
- MVP는 서버 로직(성적 계산 + 전형 배치)까지. UI 생성은 후속 단계
- 이 저장소(`every-entrance`)가 DSL/엔진 개발의 작업 공간
- 구현 과정에서 확정된 설계 (상세 근거는 CLAUDE.md):
  - DSL 산출물은 불변 `AdmissionPlan` 모델이며 **생성자에서 항상 검증**됨 — 존재하는 plan은 항상 유효
  - 모델에 람다/함수 타입 금지 — 수식도 선형 환산 파라미터(`RangeScaleFormula`)로 데이터화
  - 점수는 전부 `BigDecimal`, 반올림 정책(`RoundingPolicy`)은 plan에 선언
  - 연도별 plan은 수정이 아니라 **파일 추가** (`Plan2026.kt`, `Plan2027.kt`, …) — 과거 재현성 보존
  - 빌드 환경: Kotlin 2.3.21 / Gradle 9.6.1 (wrapper) / **JVM target 21** (server Java 25·AWS Lambda 겸용)
  - 패키지 루트: `kr.hellogsm.entrance`
- **산출물 위치 확정 (2026-07-20)**: 독립 라이브러리 유지가 아니라 `hellogsm-server-26`에 **멀티모듈로 흡수**. 근거는 ① 공유 MySQL 스키마 매핑이 3중(go-hellogsm·서버 JPA·신규 배치)이 되는 것을 막고, ② 소비자가 서버 하나뿐이라 태그 릴리스 사이클이 값을 못 사며, 원서접수(10월)·평가(11월) 성수기에 마찰만 남기기 때문. 경계는 레포가 아니라 Gradle 모듈 그래프로 강제한다. 통합 방향과 절차는 [MIGRATION.md](./MIGRATION.md)

## 진행 상황

**Phase 0·1·2 완료** (2026-07-21) — PLAN.md 8절 로드맵 기준. 엔진은 전 범위 구현되었고, 남은 것은 배포·통합 계열이다.

| 단계 | 상태 | 내용 |
|---|---|---|
| Phase 0 | ✅ 2026-07-19 | DSL + `Plan2026` + 검증 |
| Phase 1 | ✅ 2026-07-20 | `scoring` + Go 계산기 대비 golden test — `entrance-lambda` 배포는 미착수 |
| Phase 2 | 🔶 2026-07-21 | `evaluation`·`assignment` + go-hellogsm 대비 golden test — **엔진은 완료, `entrance-batch`(DB 러너) 미착수** |
| Phase 3 | ⬜ | 서버가 엔진 소비 → Go 레포 퇴역 |

### 모듈별 산출물

| 모듈 | 내용 |
|---|---|
| `entrance-dsl` | 도메인 모델(`plan/`: AdmissionPlan, Screening, Grading, Round, Policies) + `@DslMarker` 기반 빌더(`dsl/`) + `PlanValidator` (오류를 모아 한 번에 보고) |
| `entrance-plans` | `Plan2026.kt` — 2026 요강 전문 인코딩 (fallback 규칙, 동점자 체인, 결측 대체, 반올림 정책, 봉사 계단, 일정 포함) |
| `entrance-engine` | `scoring`(성적 계산 + breakdown), `evaluation`(1차/2차 선발·편입·동점자·추가모집), `assignment`(학과 배정·예비합격·중도포기 재배정) |

### 테스트 — 108개 전부 통과

| 모듈 | 테스트 |
|---|---|
| `entrance-dsl` | `PlanValidatorTest`(14), `AdmissionPlanDslTest`(6) |
| `entrance-plans` | `Plan2026Test`(18) — 요강 수치 고정 |
| `entrance-engine` | `ScoringEngineTest`(25), `EvaluationEngineTest`(20), `AssignmentEngineTest`(14), `AdditionalRecruitmentTest`(7), `GoParityGoldenTest`(2), `PostAdmissionFlowTest`(1), `BatchParityGoldenTest`(1) |

golden fixture 규모 (테스트 개수와 별개):

- scoring — `GoParityGoldenCases.kt`에 **108 케이스** (parity 96 + 요강·Go 산출이 갈리는 `specDivergenceCases` 12, 후자는 요강 기준 기대값으로 고정)
- evaluation·assignment — `golden/batch_parity.txt`에 **3 시나리오 · 지원자 366명**

두 fixture 모두 `tools/golden/*.py`(Go 로직 포팅)로 생성했다. 실제 Go 바이너리·DB 배치 대비 재검증은 Go 툴체인 확보 시 남은 과제.

### 다음 단계

**저장소 통합(MIGRATION.md M1·M2)이 `entrance-batch`보다 먼저다.** 배치는 서버의 영속성 레이어를 재사용해야 하는데, 통합 전에 만들면 자체 스키마 매핑을 짜게 되고 그것이 통합으로 없애려던 3중 매핑이다.

## 유지해야 할 기존 제약

- score-calculator를 Lambda로 분리한 이유는 **server가 다운되어도 FE의 "모의 성적 계산"이 동작해야 하기 때문** — Kotlin 전환 후에도 이 가용성 요건은 유지되어야 함 (엔진을 공용 모듈로 두고 Lambda 아티팩트를 별도 배포하는 방향)
- 배치는 Lambda를 직접 호출하지 않고, server가 미리 계산해 DB에 저장한 점수를 사용 (계산 시점과 배치 시점의 분리)
- 배치와 server는 같은 MySQL을 공유

## 미해결 질문

1. 공유 MySQL 마이그레이션 소유권 정책 — 통합 후 배치·서버가 한 레포가 되므로 범위는 좁아지지만, 스키마 변경 주체는 여전히 팀 확인 필요
2. **`hellogsm-server-26`의 레포 수명** — 이름의 `-26`이 학년도별 신규 레포를 뜻한다면 누적 자산인 `PlanXXXX.kt`가 매년 이사를 다녀야 해 통합 판단이 뒤집힌다. 첫 커밋이 2024-02이고 패키지가 `hellogsmv3`라 실제로는 여러 시즌을 한 레포로 넘겨온 것으로 보이나, 통합 착수 전 팀 확인 권장 ([MIGRATION.md](./MIGRATION.md) 사전 조건)
3. persistence 모듈 추출 범위 — `entrance-batch`가 서버 JPA 엔티티를 재사용하려면 Spring Boot 앱에서 영속성 레이어를 분리해야 함. 추출하지 않으면 통합의 이득 절반(스키마 단일 매핑)을 포기하게 됨

(해결됨) 산출물의 최종 위치 → `hellogsm-server-26`에 멀티모듈로 흡수 (2026-07-20, 위 결정 사항 참고).

(해결됨) 검정고시 봉사활동 환산식 → 교과 = (평균−50)÷50×240, 봉사 = (평균−40)÷60×30 (2026-07-20). 요강 PDF p.26 원문과 2026 시즌 Go 코드 일치 확인. 기존에 적혀 있던 `(평균−60)÷40` 계열은 PDF 추출 손상이었음.

(해결됨) parity 검증용 Go 레포 접근 → `.reference/`에 `go-hellogsm`, `go-hellogsm-score-calculator`, `go-hellogsm-ops` 확보. 단 parity 기준 커밋 고정 필요 (CLAUDE.md 주의사항 참고).

(해결됨) 졸업자의 최종 동점자 학기 순서 → **3-2를 쓰지 않는다**로 확정 (2026-07-20). 요강 명시 범위(3-1·2-2·2-1·1-2)를 그대로 따르며 현재 구현이 이미 그 상태.

(해결됨) 모의 성적 계산 가용성 요건 → 유지. `entrance-lambda`를 별도 배포 아티팩트로 존속시킨다 (2026-07-20).

(해결됨) DSL 로딩 방식: **컴파일 타임 포함**으로 확정 — plan이 코드로 컴파일되어 타입 체크·테스트·PR 리뷰를 그대로 거침.
