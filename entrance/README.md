# every-entrance

입학전형 요강을 **Kotlin DSL로 선언**하고, 공용 엔진이 성적 계산과 전형 배치를 수행하는 라이브러리입니다. hellogsm(광주소프트웨어마이스터고 입학지원시스템)의 전형 로직을 "매년 코드 수정"에서 "**연도별 plan 파일 하나의 선언**"으로 바꾸는 것이 목표입니다.

```
entrance-dsl      도메인 모델 + DSL 빌더 (순수 Kotlin, 의존성 없음)
entrance-plans    연도별 요강 선언 — Plan2026.kt
entrance-engine   해석 엔진 (성적 계산·전형 배치 — 개발 중)
```

- 프로젝트 배경: [CONTEXT.md](./CONTEXT.md) · 스펙/로드맵: [PLAN.md](./PLAN.md) · 개발 규칙: [CLAUDE.md](./CLAUDE.md)

## 빠른 시작

```bash
./gradlew build   # 전체 빌드 + 테스트
```

요강 하나는 `admissionPlan { ... }` 블록 하나로 선언합니다. 최소 구성 예시:

```kotlin
val plan = admissionPlan(year = 2027) {
    majors {
        major("SW", "소프트웨어개발과", capacity = 36)
    }
    screenings {
        regular("GEN", "일반전형")   // quota 생략 = 나머지 정원 전부
    }
    grading {
        rounding(intermediateScale = 5, resultScale = 3)
        transcript(CANDIDATE) { /* 아래 '성적 산출' 참고 */ }
    }
    rounds {
        round("FIRST", "1차 전형") {
            selectByMultiplier(1.3)
            sumScore(subjectScore, attendanceScore, volunteerScore)
        }
    }
    majorAssignment { choiceCount = 1 }
}
```

빌드 결과물은 불변 모델 `AdmissionPlan`이며, **생성되는 순간 정합성이 검증**됩니다. 정원 합계가 안 맞거나 가중치 합이 100%가 아니면 그 자리에서 `PlanValidationException`이 발생하므로, 존재하는 plan 인스턴스는 항상 유효합니다.

전체 예시는 [`entrance-plans/src/main/kotlin/kr/hellogsm/entrance/plans/Plan2026.kt`](./entrance-plans/src/main/kotlin/kr/hellogsm/entrance/plans/Plan2026.kt)가 2026 요강 전문을 인코딩한 실전 레퍼런스입니다.

---

## DSL 레퍼런스

### 학과 — `majors { }`

학과 코드, 이름, 정원을 선언합니다. 정원의 합이 곧 총 모집정원(`plan.totalCapacity`)입니다.

```kotlin
majors {
    major("SW", "소프트웨어개발과", capacity = 36)
    major("IOT", "스마트IoT과", capacity = 18)
    major("AI", "인공지능(AI)과", capacity = 18)
}
// plan.totalCapacity == 72
```

### 전형 — `screenings { }`

**정원 내**(`regular`)와 **정원 외**(`extra`) 전형을 구분해 선언합니다.

```kotlin
screenings {
    // 정원 내: quota를 생략하면 "다른 전형에 배정하고 남은 인원 전부" (Remainder)
    regular("GEN", "일반전형")

    regular("SPE", "특별전형(사회통합전형)") {
        quota(8)                  // 고정 8명
        unfilledGoesTo("GEN")     // 미충원분은 일반전형에서 추가 선발
        rejectedFallsTo("GEN")    // 탈락자는 같은 차수의 일반전형에 편입되어 함께 전형
        subType("BASIC_LIVING", "기초생활수급자")   // 세부 자격 유형 (열거만, 심사는 운영 절차)
    }

    // 정원 외: quota 선언 필수, 총정원과 별개
    extra("EXT_VETERANS", "국가보훈대상자") {
        quota(2, capPercentOfTotal = 3)   // 2명, 단 "전체 정원의 3% 이내" 상한 검증
        admitOnlyWithinFirstRoundCutline() // 1차(정원 내) 합격자 최저점 이내일 때만 정원 외로 전형
        overflowFallsTo("SPE")             // 모집범위 초과 시 사회통합전형에 편입
    }
}
```

전형 간 이동(fallback) 규칙 세 가지가 1급 개념입니다:

| 선언 | 의미 |
|---|---|
| `unfilledGoesTo(code)` | 이 전형의 **미충원 인원**을 대상 전형에서 추가 선발 |
| `rejectedFallsTo(code)` | 이 전형의 **탈락자**를 같은 차수의 대상 전형에 포함하여 전형 |
| `overflowFallsTo(code)` | 정원 외 **모집범위 초과자**를 대상 전형에 편입 |

실제 모집인원은 `plan.resolvedQuota(code)`로 조회합니다 — Remainder는 `총정원 − 고정 정원 합`으로 풀립니다 (위 예시에서 `resolvedQuota("GEN") == 64`).

### 성적 산출 — `grading { }`

졸업 구분(`CANDIDATE` 졸업예정 / `GRADUATE` 졸업 / `GED` 검정고시)별로 산출 스키마를 선언합니다.

#### 반올림 정책 (필수)

점수 연산은 전부 `BigDecimal`이며, 요강에 명시된 자릿수 규칙을 plan에 선언합니다.

```kotlin
// 중간값: 소수점 여섯째 자리에서 반올림해 다섯째 자리까지(scale 5)
// 결과값: 소수점 넷째 자리에서 반올림(scale 3)
rounding(intermediateScale = 5, resultScale = 3)   // mode 기본값: HALF_UP
```

#### 내신 기반 — `transcript(type) { }`

```kotlin
transcript(CANDIDATE) {
    generalSubjects(max = 180) {
        achievement(A to 5, B to 4, C to 3, D to 2, E to 1)  // 성취도 → 환산점수
        semester(1, 2, points = 18)   // 1학년 2학기 18점
        semester(2, 1, points = 45)
        semester(2, 2, points = 45)
        semester(3, 1, points = 72)   // 배점 합은 max(180)와 일치해야 함 — 검증됨
        // 성적 없는 학기의 대체 순서: 같은 학년 다른 학기 → 차상위 학년 → 차하위 학년
        missingSemester(SAME_YEAR_OTHER_SEMESTER, UPPER_YEAR, LOWER_YEAR)
    }

    artsSubjects(max = 60) {
        achievement(A to 5, B to 4, C to 3)   // 예체능 3등급 — 3년 평균 × 60점
    }

    attendance(max = 30) {
        latenessPerAbsenceDay = 3    // 미인정 지각·조퇴·결과 3회 = 결석 1일 (버림)
        deductionPerAbsenceDay = 3   // 환산 결석 1일당 −3점
        zeroFromAbsenceDays = 10     // 10일 이상이면 0점
        missingYearDefault = 5       // 출결 자료가 없는 학년의 기본점
    }

    volunteer(maxPerYear = 10) {     // years 기본값: 1, 2, 3학년
        step(minHours = 7, points = 10)   // 연 7시간 이상 → 10점 (계단 함수)
        step(minHours = 6, points = 8)
        step(minHours = 5, points = 6)
        step(minHours = 4, points = 4)
        floor(2)                          // 모든 계단 미달(3시간 이하) → 2점
        missingYearDefault = 2
    }
}
```

졸업자(`GRADUATE`)는 학기 배점 테이블만 바꿔 같은 구조로 선언합니다 (2026 기준: 2학년 36+36, 3학년 54+54).

#### 수식 기반 — `formula(type) { }` (검정고시)

수식은 코드(람다)가 아니라 **선형 환산 파라미터**로 선언합니다: `score = (입력 − minInput) ÷ (maxInput − minInput) × maxScore`

```kotlin
formula(GED) {
    subjects(minInput = 60, maxInput = 100, maxScore = 240)  // (평균 − 60) ÷ 40 × 240
    attendanceFixed(30)                                      // 출석은 30점 고정
    volunteer(minInput = 60, maxInput = 100, maxScore = 30)
}
```

> ⚠️ 2026 plan의 검정고시 봉사 환산식은 기존 Go 구현과 대조 전의 **가정값**입니다 (`Plan2026.kt`의 TODO 참고).

### 전형 절차 — `rounds { }`

N차 전형을 순서대로 선언합니다. 선언 순서가 곧 진행 순서입니다.

```kotlin
rounds {
    round("FIRST", "1차 전형(서류전형)") {
        selectByMultiplier(1.3)   // 전형별 모집정원 × 1.3배수 이내 선발
        // allCut = true (기본값): 학과 구분 없는 총원제 선발
        sumScore(subjectScore, attendanceScore, volunteerScore)   // 단순 합산 = 300점
        tiebreak {                // 동점자 처리 — 선언 순서가 우선순위
            byGeneralSubjectScore()                    // ① 예체능 제외 일반교과
            bySemesters(3 to 1, 2 to 2, 2 to 1, 1 to 2) // ② 학기 성적 순차 비교
            byNonSubjectScore()                        // ③ 비교과(출석+봉사)
        }
    }

    round("SECOND", "2차 전형(역량검사·심층면접)") {
        selectByCapacity()        // 모집정원만큼 선발
        // absentPolicy = EXCLUDE (기본값): 미응시자는 전형 대상 제외

        // 수동 입력 점수(운영자가 숫자로 입력)는 여기서 선언
        val competency = manualScore("COMPETENCY", "역량검사", max = 100)
        val interview = manualScore("INTERVIEW", "심층면접", max = 100)

        weightedScore(max = 100) {  // 가중 합산 — 비율 합 100% 검증됨
            part(roundScore("FIRST"), weightPercent = 50, normalizeTo = 100) // 1차 300점 → 100점 정규화 후 50%
            part(competency, weightPercent = 30)
            part(interview, weightPercent = 20)
        }
        tiebreak {
            byManualScore("COMPETENCY")
            byManualScore("INTERVIEW")
            byGeneralSubjectScore()
            bySemesters(3 to 1, 2 to 2, 2 to 1, 1 to 2)
            byNonSubjectScore()
        }
    }
}
```

점수 구성요소로 쓸 수 있는 것:

| 구성요소 | 의미 |
|---|---|
| `subjectScore` | 교과 성적 (일반교과 + 예체능) |
| `attendanceScore` | 출석 성적 |
| `volunteerScore` | 봉사활동 성적 |
| `roundScore("FIRST")` | 이전 차수의 총점 (이전 차수만 참조 가능 — 검증됨) |
| `manualScore(code, name, max)` | 수동 입력 점수 — 코드는 plan 전체에서 유일해야 함 |

### 학과 배정 · 예비합격 · 추가모집 · 일정

```kotlin
majorAssignment {
    choiceCount = 3               // 3지망까지 전부 기재 필수
    extraScreeningCapPerMajor = 2 // 정원 외 합격자는 학과당 최대 2명
}

waitlist {
    percentOfTotal(3)   // 모집정원의 3% 범위에서 발표
    from("GEN")         // 일반전형 불합격자 중 고득점 순
}

additionalRecruitment {   // 모집정원 미달 시
    screening("GEN")        // 일반전형으로만
    basedOnRound("FIRST")   // 1차 환산점수만으로 선발
}

schedule {   // MVP에서는 참조 데이터 (UI 단계에서 상태 전이에 활용 예정)
    event("APPLICATION", "원서 접수", LocalDate.of(2025, 10, 20), LocalDate.of(2025, 10, 23))
    event("FIRST_RESULT", "1차 합격자 발표", LocalDate.of(2025, 10, 28))  // end 생략 = 하루짜리
}
```

---

## 검증 — 잘못된 plan은 만들어지지 않는다

`AdmissionPlan`은 생성자에서 `PlanValidator`를 거치며, **모든 오류를 모아 한 번에** 보고합니다.

```kotlin
val plan = admissionPlan(year = 2027) {
    majors { major("SW", "소프트웨어개발과", capacity = 10) }
    screenings {
        regular("GEN", "일반전형") { quota(8) }
        regular("SPE", "특별전형") { quota(5) }   // 8 + 5 > 10 !
    }
    // ...
}
// PlanValidationException: 입학전형 plan 검증 실패 (1건):
//  - 정원 내 전형의 고정 정원 합(13)이 총정원(10)을 초과함
```

검증되는 것들 (일부):

- 학과/전형/차수 코드 유일성, 정원 내 고정 정원 합 ≤ 총정원, Remainder 전형 최대 1개
- 정원 외 전형의 `capPercentOfTotal` 상한 초과 여부 (인원수 기준 내림)
- fallback 대상 전형(`unfilledGoesTo` 등)의 존재 여부
- 일반교과 학기 배점 합 = 만점, 봉사 계단의 내림차순 정렬
- 가중 점수 비율 합 = 100%, `roundScore`가 이전 차수만 참조하는지
- `tiebreak`의 `byManualScore`가 해당 차수에 선언된 점수를 참조하는지
- 지망 학과 수 ≤ 학과 수, 예비합격/추가모집이 참조하는 전형·차수의 존재 여부

빌더 수준의 필수값 누락(예: `grading` 블록 생략, 정원 외 전형의 `quota` 생략)은 `IllegalArgumentException`으로 즉시 실패합니다.

## 만들어진 plan 사용하기

```kotlin
import kr.hellogsm.entrance.plans.plan2026

plan2026.totalCapacity                 // 72
plan2026.resolvedQuota("GEN")          // 64 (Remainder가 풀린 값)
plan2026.major("SW").name              // "소프트웨어개발과"
plan2026.screening("SPE").unfilledGoesTo   // "GEN"

val second = plan2026.round("SECOND")
second.manualScores                    // [COMPETENCY(100점), INTERVIEW(100점)]
second.tiebreakers                     // 동점자 기준 체인 (우선순위 순)
```

## 새 학년도 요강 추가하기

1. `entrance-plans`에 `PlanXXXX.kt`를 **새 파일로 추가**합니다 (기존 연도 파일은 재현성을 위해 수정하지 않습니다). 가장 빠른 방법은 직전 연도 파일을 복사해 바뀐 수치만 고치는 것입니다.
2. 요강 원문(PDF)을 `.reference/`에 두고, plan의 모든 수치는 그 문서를 근거로 합니다.
3. `PlanXXXXTest`를 함께 작성해 요강의 핵심 수치(정원, 배점, 가중치, 동점자 순서)를 테스트로 고정합니다.
4. 기존 DSL로 표현할 수 없는 새 규칙이 생겼다면 — 그것이 이 구조의 존재 이유입니다. plan 파일에 우회 코드를 넣지 말고 `entrance-dsl`의 모델·빌더·검증기를 확장하세요.

## 개발

```bash
./gradlew build                                            # 전체 빌드 + 테스트
./gradlew :entrance-plans:test                             # 특정 모듈 테스트
./gradlew :entrance-dsl:test --tests '*PlanValidatorTest*' # 특정 테스트 클래스
```

- Kotlin 2.3.21 · Gradle wrapper 9.6.1 · JVM target 25
- 현재 상태: **Phase 0~2 완료** — DSL + 2026 plan + 검증, 엔진 전 범위(`scoring`·`evaluation`·`assignment`)와 기존 Go 구현 대비 golden test, `entrance-batch`(DB 러너), `entrance-lambda`(모의 성적 계산 API) 구현·테스트·CI/CD까지 완료.
- 남은 것: `entrance-lambda`의 AWS 실배포(함수·API Gateway는 인프라 작업, 코드 밖), 실 배치 대비 재검증(Go 툴체인 필요), 서버가 엔진을 실제로 소비하도록 전환. 전체 로드맵은 [PLAN.md](./PLAN.md) 8절, 저장소 통합 계획은 [MIGRATION.md](./MIGRATION.md) 참고.
