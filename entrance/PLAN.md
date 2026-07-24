# PLAN.md — 입학전형 Kotlin DSL 구현 계획

> 배경과 문제 인식은 [CONTEXT.md](./CONTEXT.md) 참고. 이 문서는 스펙 정의와 실행 계획을 다룬다.

## 1. 목표와 비목표

### 목표 (1차 MVP)

- 입학전형 요강을 **선언적 Kotlin DSL**로 기술하고, 이를 해석하는 **엔진**이 다음을 수행한다:
  1. **성적 계산** — 졸업예정자/졸업자/검정고시별 내신 환산 (현 `go-hellogsm-score-calculator` 대체)
  2. **전형 배치** — 1차/2차 평가, 최종 학과 배정, 추가모집 (현 `go-hellogsm` 대체)
- 연도별 요강 변경이 **DSL 파일 하나의 수정**으로 수렴하는 구조
- 기존 Go 구현과의 **결과 동등성(parity) 검증**을 거친 뒤 전환

### 비목표 (후속 단계)

- UI(FE 폼/화면) 자동 생성 — DSL 스키마는 이를 염두에 두고 설계하되 구현하지 않음
- 제출 서류 체크리스트, 전형 일정 기반 상태 전이 자동화
- server(Spring) 자체의 재작성 — server는 엔진을 라이브러리로 소비할 뿐

## 2. 핵심 설계 원칙

1. **DSL은 데이터, 엔진은 로직.** DSL 빌더의 산출물은 임의 코드가 아닌 **불변 도메인 모델(`AdmissionPlan`)**이다. 엔진은 이 모델만 해석한다. 람다를 받는 escape hatch는 최소화한다 — 람다가 섞이는 순간 "설정으로서의 요강"이 아니라 "코드가 다른 파일로 이사한 것"이 된다.
2. **요강에 없는 판단은 인코딩하지 않는다.** "입학전형위원회에서 심의·결정" 같은 조항(예: 2011년 이전 졸업자 석차 환산)은 규칙화하지 말고 **수동 점수 오버라이드** 입력으로 처리한다.
3. **결정성과 감사 가능성.** 같은 입력 + 같은 plan 버전 = 항상 같은 출력. 배치 결과에는 사용된 plan 버전을 기록한다.
4. **정밀도는 명세의 일부.** 모든 점수 연산은 `BigDecimal`, 반올림 자릿수/방식은 DSL에 선언한다 (요강이 자릿수를 명시하므로).
5. **연도별 plan은 파일로 공존.** `Plan2026.kt`, `Plan2027.kt`처럼 나란히 두어 과거 재현과 diff 리뷰가 가능하게 한다.

## 3. 아키텍처

```
┌────────────────────────────────────────────────────┐
│ entrance-plans        연도별 요강 DSL 선언           │
│   └─ Plan2026.kt  (data만, 로직 없음)               │
├────────────────────────────────────────────────────┤
│ entrance-dsl          DSL 빌더 + 도메인 모델          │
│   (순수 Kotlin, 외부 의존성 0)                       │
├────────────────────────────────────────────────────┤
│ entrance-engine       해석 엔진 (순수 함수)           │
│   ├─ scoring     성적 계산                          │
│   ├─ evaluation  1차/2차 선발, 동점자, fallback      │
│   └─ assignment  학과 배정, 예비합격, 추가모집        │
├────────────┬───────────────┬───────────────────────┤
│ entrance-  │ entrance-     │ hellogsm-server-26     │
│ lambda     │ batch         │ (엔진을 의존성으로 소비) │
│ 모의성적계산 │ 배치 러너(DB)  │                        │
└────────────┴───────────────┴───────────────────────┘
```

- **entrance-dsl / entrance-engine / entrance-plans**: 순수 Kotlin, DB·프레임워크 무관. 단위 테스트가 가장 쉬운 곳에 로직을 모은다.
- **entrance-lambda**: 모의 성적 계산 API. server 다운 시에도 동작해야 하는 기존 가용성 요건을 계승하므로 **별도 배포물 유지** (JVM cold start는 SnapStart로 완화, 필요시 GraalVM native 검토).
- **entrance-batch**: 현 `go-hellogsm` 역할. DB 읽기 → 엔진 호출 → 결과 기록. 배치는 지금처럼 DB에 사전 계산된 점수를 사용하며 Lambda를 호출하지 않는다.
- **server 통합**: **아티팩트 발행이 아니라 저장소 통합으로 확정** (2026-07-20). 위 모듈 전체가 `hellogsm-server-26`에 Gradle 멀티모듈로 흡수되며, 엔진은 서버·persistence 모듈을 의존성으로 선언하지 않는 것으로 "엔진은 DB를 모른다"를 컴파일 타임에 강제한다. 절차는 [MIGRATION.md](./MIGRATION.md).

### DSL 로딩 방식 (확정: 컴파일 타임 포함)

**컴파일 타임 포함.** plan이 코드로 컴파일되므로 타입 체크·테스트·리뷰가 PR 흐름에 그대로 태워진다. 요강은 1년에 한 번 바뀌므로 런타임 리로딩(`.kts` 스크립팅)의 이점이 없고, 스크립트 로딩은 보안·버전 관리 문제만 더한다.

## 4. 도메인 모델 스펙

2026 요강 전체를 표현할 수 있어야 하며, 아래 각 항목이 모델의 최상위 개념이 된다.

### 4.1 학과 (Major)

| 필드 | 예시 (2026) |
|---|---|
| code, name | SW / 소프트웨어개발과 |
| capacity | 36 / 18 / 18 (계 72) |

### 4.2 전형 (Screening)

- **정원 내**: 일반전형(64), 특별전형·사회통합(8)
- **정원 외**: 국가보훈(2명, 총정원 3% 이내), 특례입학(1명, 총정원 2% 이내)
- 정원 표현: 고정 인원 + 총정원 대비 % 상한의 조합 지원
- 세부 자격 유형(기초생활수급자 등 5종, 특례 제1~4호)은 열거형으로만 모델링 (자격 심사는 서버 로직 밖)
- **fallback 규칙 (필수)**:
  - 특별전형 **미충원분** → 일반전형에서 추가 선발
  - 특별전형 **탈락자** → 같은 차수의 일반전형에 포함하여 전형 (1차·2차 각각)
  - 정원외 전형 → **1차 합격자 최저점 이내**인 경우에만 정원외 범위 내 전형, 모집범위 초과 시 사회통합전형에 편입

### 4.3 졸업 구분 (GraduationType)과 성적 산출 (Grading)

`CANDIDATE`(졸업예정) / `GRADUATE`(졸업) / `GED`(검정고시)별로 산출 스키마가 다르다.

**공통 구조** — 총점 300 = 교과 240 (일반교과 180 + 예체능 60) + 비교과 60 (출석 30 + 봉사 30)

**일반교과 (학기 가중치 테이블)**

| | 1-2 | 2-1 | 2-2 | 3-1 | 3-2 |
|---|---|---|---|---|---|
| 졸업예정자 | 18 | 45 | 45 | 72 | — |
| 졸업자 | — | 36 | 36 | 54 | 54 |

- 학기별 산출: 과목별 성취도 환산점수 합 ÷ (과목수 × 5) → **소수점 여섯째 자리 반올림, 다섯째 자리까지** → 학기 배점 곱
- 성취도 환산: A=5, B=4, C=3, D=2, E=1 (평어 병기: 수→A … 가→E)
- 결과값: **소수점 넷째 자리에서 반올림**
- **결측 학기 대체 규칙** (우선순위): ① 같은 학년 다른 학기 ② 차상위 학년 학기별 적용 ③ 차하위 학년 학기별 적용

**예체능 교과**: A=5, B=4, C=3 / 3년간 성취도 환산점수 평균(소수점 넷째 자리 반올림) × 60

**비교과**
- 출석(30): 환산 결석일수 = 미인정 결석 + ⌊(미인정 지각+조퇴+결과) ÷ 3⌋. 0일 30점, 1일당 −3점, 10일 이상 0점. 전 학년 합산
- 봉사(30, 학년별 10): 연 7h 이상 10 / 6h 8 / 5h 6 / 4h 4 / ≤3h 2 — 계단함수
- **결측 학년 기본점**: 출석 5점, 봉사 2점 (조기진급·복학·해외귀국 등)

**검정고시(GED)**: 교과 = (평균점수 − 50) ÷ 50 × 240, 출석 = 30점 고정, 봉사 = (평균점수 − 40) ÷ 60 × 30. 음수는 0으로 처리(기존 Go 구현 기준). — 계수는 요강 PDF p.26 수식 원문(내장 폰트 글리프 해독)과 2026 시즌 Go score-calculator 코드가 일치함을 확인하여 확정.

**기준일 정책**: 졸업예정자 교과는 3-1까지·비교과는 9/30 기준, 졸업자는 졸업일까지 — plan에 선언 (배치 로직에는 영향 없고 입력 검증·문서화용)

### 4.4 전형 절차 (Rounds)

N차 전형의 리스트. 각 round는:

- **선발 기준**: 배수(1차: 모집정원 × 1.3) 또는 정원(2차: 모집정원)
- **총원제(All-cut)**: 1·2차는 학과 구분 없이 전형별 총원 기준
- **점수 구성 (composite)**:
  - 1차: 교과 + 출석 + 봉사 = 300점
  - 2차: 1차 성적(300→100 정규화) 50% + 역량검사(100) 30% + 심층면접(100) 20%
- **수동 입력 점수 (ExternalScore)**: 역량검사·심층면접처럼 운영자가 숫자로 입력하는 점수의 선언 (이름, 만점, 소속 round)
- **동점자 처리 (ordered comparator chain)**:
  - 1차: ① 교과성적(예체능 제외) ② 학기 성적 3-1 → 2-2 → 2-1 → 1-2 순 ③ 비교과
  - 최종: ① 역량검사 ② 심층면접 ③~⑤ 1차와 동일
- **불참 처리**: 미응시자는 전형 대상 제외

### 4.5 학과 배정 (MajorAssignment)

- 지망 수: 3 (전부 기재 필수)
- 일반·특별 통합, 성적 상위순으로 1지망 → 2지망 → 3지망 배정
- 정원외 합격자: 성적순 + 희망 고려, **학과당 최대 2명**
- 정원 미달 시 지원과 변경 배정 가능 (운영 판단 개입 지점 — 자동화하지 않고 오버라이드로)

### 4.6 예비 합격자 / 추가모집

- 예비합격: 모집정원의 3% 범위, 일반전형 불합격자 중 고득점 순
- 추가모집(`RE_EVALUATE`): 정원 미달 시에만, **일반전형만**, **1차 환산점수만**으로 선발

### 4.7 일정 (Schedule)

접수·발표·검사·등록 기간을 선언만 해 둔다 (MVP에서는 참조 데이터, UI 단계에서 상태 전이에 활용).

## 5. DSL 스케치 (API 방향성 예시 — 구현 시 조정)

```kotlin
val plan2026 = admissionPlan(year = 2026) {
    majors {
        major("SW", "소프트웨어개발과", capacity = 36)
        major("IOT", "스마트IoT과", capacity = 18)
        major("AI", "인공지능(AI)과", capacity = 18)
    }

    screenings {
        regular("GEN", "일반전형") { quota = remainder() }          // 특별 미충원분 흡수
        regular("SPE", "사회통합전형") {
            quota = fixed(8)
            unfilledGoesTo("GEN"); rejectedFallsTo("GEN")
        }
        extra("EXT_VET", "국가보훈대상자") {
            quota = fixed(2) cappedAt totalCapacityPercent(3)
            admitOnlyWithin = firstRoundCutline                     // 1차 합격 최저점 이내
            overflowFallsTo("SPE")
        }
        extra("EXT_SPC", "특례입학대상자") {
            quota = fixed(1) cappedAt totalCapacityPercent(2)
            admitOnlyWithin = firstRoundCutline
            overflowFallsTo("SPE")
        }
    }

    grading {
        rounding { intermediate = halfUp(scale = 5); result = halfUp(scale = 3) }

        graduationType(CANDIDATE) {
            generalSubjects(max = 180) {
                achievement(A to 5, B to 4, C to 3, D to 2, E to 1)
                semester(1 to 2, points = 18)
                semester(2 to 1, points = 45); semester(2 to 2, points = 45)
                semester(3 to 1, points = 72)
                missingSemester(SAME_YEAR, UPPER_YEAR, LOWER_YEAR)
            }
            artsSubjects(max = 60) { achievement(A to 5, B to 4, C to 3); averageScaledTo(60) }
            attendance(max = 30) {
                absenceEquivalent = lateness / 3                    // 버림
                deductionPerDay = 3; zeroFrom = 10
                missingYearDefault = 5
            }
            volunteer(maxPerYear = 10) {
                step(hours = 7, points = 10); step(6, 8); step(5, 6); step(4, 4); floor(2)
                missingYearDefault = 2
            }
        }
        graduationType(GRADUATE) { /* 학기 테이블만 다름: 2학년 36+36, 3학년 54+54 */ }
        graduationType(GED) {
            subjectsByFormula(max = 240) { avg -> (avg - 60) / 40 * 240 }
            attendanceFixed(30)
            volunteerByFormula(max = 30) { avg -> TODO("기존 Go 구현과 대조 후 확정") }
        }
    }

    rounds {
        round("FIRST", "서류전형") {
            selectBy = multiplier(1.3); allCut = true
            score = sum(subjectScore, attendanceScore, volunteerScore)   // 300
            tiebreak {
                by(generalSubjectScore)                                   // 예체능 제외
                bySemester(3 to 1, 2 to 2, 2 to 1, 1 to 2)
                by(nonSubjectScore)
            }
        }
        round("SECOND", "역량검사·심층면접") {
            selectBy = capacity(); allCut = true
            val competency = manualScore("COMPETENCY", max = 100)
            val interview = manualScore("INTERVIEW", max = 100)
            score = weighted(max = 100) {
                previousRound(normalizeTo = 100) at 50.pct
                competency at 30.pct
                interview at 20.pct
            }
            absentPolicy = EXCLUDE
            tiebreak {
                by(competency); by(interview)
                by(generalSubjectScore); bySemester(3 to 1, 2 to 2, 2 to 1, 1 to 2); by(nonSubjectScore)
            }
        }
    }

    majorAssignment { choices = 3; extraScreeningCapPerMajor = 2 }
    waitlist { totalCapacityPercent(3); from("GEN") }
    additionalRecruitment { screening("GEN"); basedOn = round("FIRST") }
}
```

## 6. 엔진 책임 정의

| 컴포넌트 | 입력 | 출력 |
|---|---|---|
| scoring | plan + 지원자 성적 원본(성취도/출결/봉사 or 검정고시 평균) | 학기별·영역별 환산점수 breakdown + 총점 |
| evaluation | plan + 지원자 목록(사전 계산 점수 + 수동 점수) + 대상 round | 전형별 합격/탈락/편입 결과 (fallback 적용 후) |
| assignment | plan + 최종 합격자(지망 포함) | 학과 배정 결과 + 예비합격자 목록 |

- 모든 출력에 **산출 근거(breakdown)** 포함 — 이의 제기 대응과 admin 노출을 위해
- 오버라이드 입력 채널: 위원회 결정 사항(특수 케이스 성적, 지원과 변경 배정)은 엔진 입력에 오버라이드로 주입하고 결과에 그 사실을 표시

## 7. 마이그레이션 및 검증 전략

전환의 최대 리스크는 **기존 Go 구현과의 결과 불일치**다. 실제 합격/불합격이 갈리는 시스템이므로 단계마다 parity를 검증한다.

1. **Golden test**: `go-hellogsm-ops`의 `generate-dml`로 대량의 mock 지원자를 생성 → 기존 Go 계산기/배치와 Kotlin 엔진에 동일 입력 → 결과 전수 비교. 특히 경계값(결측 학기, 결석 10일, 동점자, 정원외 편입)을 property-based로 보강. 현재는 실제 `generate-dml` 대신 Go 로직을 Python으로 직접 포팅하는 방식으로 대체 — Go 툴체인 확보 시 실제 바이너리/DB 배치 대비 재검증이 남은 항목
   - scoring은 1차로 `tools/golden/generate_golden.py`(2026 시즌 Go 코드의 big.Rat 산술 포팅)로 108케이스 고정 완료. Go 툴체인 확보 시 실제 바이너리 대비 재검증 권장. **parity 기준은 커밋 4e8d668 이전 코드** — HEAD는 2027 시즌 배점으로 변경됨
   - **확인된 불일치 ③ 중도포기 재배정 시 정원 외 빈자리 계산 (2026-07-21)**: go-hellogsm은 정원 내 포기자가 생겨도 같은 학과의 정원 외 자리가 함께 열리도록 계산한다(`major_assignment_job.go`의 `ExtraMajor-extraCount-normalCount`). 정원 외 상한(학과당 2명)은 정원 내 정원과 독립이므로 학과당 2명을 넘길 수 있는 버그이며, 엔진은 각 풀의 빈자리만 채운다.
   - **검증 범위 밖**: 추가모집(`RE_EVALUATE`)은 go-hellogsm에 대응 배치가 없어(원서 상태값만 존재) parity 대상이 아니다 — 요강 8-바만을 근거로 구현했고 `AdditionalRecruitmentTest`가 요강 조항을 고정한다.
   - evaluation·assignment는 `tools/golden/generate_batch_golden.py`(2026 시즌 go-hellogsm 배치 로직 포팅)로 3시나리오·366명 고정 완료 (2026-07-21). **parity 기준은 커밋 da09df4 이전 상수** — HEAD는 2027 시즌으로 추정되는 정원 변경(72→64명 등)이 반영됨. 최종 동점자 순서(`FinalTieBreakerQuery`)는 f4b17bc(2026-07-15)에서 요강 순서(역량검사→심층면접→교과…)로 수정됐는데, 이 커밋은 da09df4보다 먼저 머지되어 baseline에 이미 포함됨 — 단, f4b17bc 머지 시점이 2026 시즌 실제 원서접수(2025-10)·평가(2025-11) 이후이므로 **그 해 실제 배치가 돌 때는 역순(버그) 상태였을 가능성이 있다**. 요강이 정답이므로 엔진은 수정된(spec) 순서를 따르고 이 판단을 기록으로 남긴다.
2. **정밀도 주의**: Go 구현이 float64를 쓴다면 요강의 자릿수 규칙과 미세하게 다를 수 있음 — 불일치 발견 시 **요강이 정답 기준**이며, 기존 구현의 버그로 판명되면 문서화 후 요강 쪽으로 수정
   - **확인된 불일치 ① 학기 몫 중간 반올림 (2026-07-20)**: 요강은 학기 몫(성취도 합 ÷ 과목수×5)을 "소수점 여섯째 자리에서 반올림하여 다섯째 자리까지" 구한 뒤 배점을 곱하라고 명시하지만, Go는 중간 반올림 없이 정확한 유리수로 계산 후 결과만 반올림한다. 무작위 표본의 약 13%에서 학기 점수가 ±0.001 갈림 (과목수가 3·6·7·9·11·12 등 10^5의 약수가 아닌 분모일 때). 엔진은 요강을 따르며, 갈리는 케이스는 `GoParityGoldenCases.kt`의 `specDivergenceCases`에 요강 기준 기대값으로 고정.
   - **확인된 불일치 ② 검정고시 평균의 float64 입력**: Go Lambda는 평균점수를 JSON float64로 받아 `big.Rat`으로 변환하므로 소수 셋째 자리 이상 입력에서 이진 오차가 반올림 경계(예: ÷2 결과가 정확히 .0005)를 뒤집을 수 있다. 엔진은 BigDecimal로 십진 정확 계산. golden fixture는 소수 둘째 자리 이하 평균만 사용.
3. **병행 운전**: 실전 투입 전 시즌(또는 과거 시즌 데이터)에서 두 구현을 병행 실행해 비교 리포트 생성
4. 전환 완료 후 Go 레포 아카이브

## 8. 단계별 로드맵

| 단계 | 내용 | 완료 기준 |
|---|---|---|
| **Phase 0** | 레포 세팅(Gradle 멀티모듈, Kotlin 2.x), `entrance-dsl` 도메인 모델 + 빌더, `Plan2026.kt` 인코딩 | 2026 요강 전체가 DSL로 표현되고 plan 검증 테스트(정원 합계, 가중치 합 = 만점 등) 통과 |
| **Phase 1** | `entrance-engine/scoring` 구현 + Go 계산기 대비 golden test, `entrance-lambda` 배포 | 모의 성적 계산 API가 기존 Lambda와 동일 응답 — **scoring·golden test 완료(2026-07-20), `entrance-lambda` 구현·테스트·CI/CD 완료(2026-07-24). 남은 것은 AWS 실배포(함수·API Gateway는 인프라 작업)뿐** |
| **Phase 2** | `entrance-engine/evaluation·assignment` + `entrance-batch` 구현, `go-hellogsm` 대비 parity | 1차/2차/최종배정/추가모집 배치 결과 전수 일치 (의도된 수정 제외) — **엔진 전 범위 완료(2026-07-21): 1차/2차 선발·편입·동점자, 최초 학과 배정, 예비합격, 중도포기 재배정, 추가모집 + go-hellogsm 대비 golden test. 남은 것: `entrance-batch`(DB 러너), 실 배치 대비 재검증(Go 툴체인 필요)** |
| **Phase 3** | server(`hellogsm-server-26`)가 엔진을 모듈 의존성으로 소비, Go 레포 퇴역 | 운영 트래픽이 Kotlin 엔진만 사용 |
| **Phase 4+** (비전) | admin에서 plan 시뮬레이션/미리보기, DSL 스키마 기반 FE 폼 생성 | — |

> **순서 주의**: 저장소 통합([MIGRATION.md](./MIGRATION.md) M1·M2)은 Phase 2의 남은 항목인 `entrance-batch`보다 **먼저** 진행한다. 배치는 서버의 영속성 레이어를 재사용해야 하는데, 통합 전에 만들면 공유 MySQL에 대한 자체 스키마 매핑을 짜게 되고 그것이 통합으로 없애려던 3중 매핑이다.

## 9. 리스크

- **DSL 과설계**: 표본이 요강 1개년뿐이므로, 일반화는 2026 요강 + 기존 코드에서 확인되는 변형 축까지만. "미래에 필요할지도 모르는" 축은 넣지 않는다 (필요해지는 해에 DSL을 확장하는 것이 이 구조의 존재 이유)
- **DSL 표현력 한계**: 위원회 재량 조항은 애초에 오버라이드로 설계 (원칙 2)
- **Lambda cold start**: JVM 전환으로 응답 지연 증가 가능 — SnapStart 우선 적용, 미달 시 GraalVM native 검토
- **공유 DB 스키마 결합**: 배치·server가 같은 MySQL을 공유하므로, 엔진은 DB를 모르게 하고(`entrance-batch`만 접근) 스키마 변경을 최소화
- **parity 불일치의 해석 비용**: 요강 vs 기존 구현이 다른 경우의 판정 절차를 미리 정함 (7절 2항)

## 10. Open Questions

1. ~~**산출물 위치**~~ → **해소(2026-07-20)**: `hellogsm-server-26`에 Gradle 멀티모듈로 **흡수**한다. 기존에 적어둔 "배치·Lambda도 소비자이므로 독립 유지" 논거는 성립하지 않았다 — 그 둘은 이 레포에 들어올 모듈이라 실제 외부 소비자는 서버 하나뿐이다. 방향은 `every-entrance` → 서버 레포(`git subtree`), 반대 방향은 CI/CD·CodeDeploy 이전 비용으로 기각. 상세는 [MIGRATION.md](./MIGRATION.md).
2. ~~**모의 성적 계산 가용성 요건 유지 여부**~~ → **해소(2026-07-20)**: 요건 유효. `entrance-lambda`를 별도 배포 아티팩트로 유지한다. 저장소 통합과 런타임 결합은 별개이며, 같은 레포의 모듈이어도 Lambda는 독립 배포된다.
3. ~~**검정고시 봉사 환산식**: 정확한 계수를 `go-hellogsm-score-calculator` 코드에서 확인 필요~~ → **해소(2026-07-20)**: 봉사 = (평균 − 40) ÷ 60 × 30, 교과 = (평균 − 50) ÷ 50 × 240. 요강 PDF p.26 원문과 Go 구현이 일치. (기존 PLAN.md의 "(평균 − 60) ÷ 40 × 240"이 추출 손상으로 잘못된 값이었음)
4. ~~**졸업자의 최종 동점자 학기 순서**~~ → **해소(2026-07-20)**: 졸업자도 **3-2를 쓰지 않는다.** 요강 명시 범위(3-1·2-2·2-1·1-2)를 그대로 따른다. `Plan2026.kt`가 이미 그 상태이고 `Tiebreakers`는 plan에 선언된 학기만 읽으므로 코드 변경 없음 — 향후 plan에서도 3-2를 추가하지 않는다.
5. **DB 마이그레이션 소유권**: 공유 MySQL의 스키마 변경 주체 (기존에도 "팀에 문의" 상태). 저장소 통합 후 배치·서버가 한 레포가 되므로 범위는 좁아지지만 정책 자체는 미결.
6. **`hellogsm-server-26`의 레포 수명**: 이름의 `-26`이 학년도별 신규 레포를 뜻한다면 누적 자산인 `PlanXXXX.kt`가 매년 이사를 다녀야 해 1번 결정이 뒤집힌다. 첫 커밋 2024-02·패키지 `hellogsmv3`로 보아 여러 시즌을 한 레포로 넘겨온 것으로 보이나, 통합 착수 전 팀 확인 필요 ([MIGRATION.md](./MIGRATION.md) 3절 사전 조건).
