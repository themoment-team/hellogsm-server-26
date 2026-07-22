# 요강을 DSL로 선언하기 (entrance-dsl · entrance-plans)

요강(모집 정원·전형·배점·동점자 기준·일정)은 코드 로직이 아니라 **선언적 데이터**로 표현한다.
`entrance-dsl`이 그 문법(type-safe builder)을 제공하고, `entrance-plans`가 실제 학년도 요강을
그 문법으로 적어 둔 데이터다.

- 유일한 근거 문서는 **요강 PDF**다. `entrance-plans`의 수치는 PDF와 1:1로 대조된다.
- 산출물은 불변 `AdmissionPlan` 모델이며, **생성 시점에 검증**된다(`PlanValidator`). 존재하는
  plan은 항상 유효하다. 검증 실패 시 오류를 모아 `PlanValidationException`으로 한 번에 던진다.

## 전체 골격

`entrance/entrance-plans/.../Plan2026.kt`가 정본 예시다. 최상위는 `admissionPlan(year) { }` 하나이고
그 안에 블록들이 들어간다.

```kotlin
val plan2026 = admissionPlan(year = 2026) {
    majors { /* 학과와 정원 */ }
    screenings { /* 전형과 정원·편입 규칙 */ }
    grading { /* 성적 산출 규칙(졸업구분별) */ }
    rounds { /* 차수(1차·2차)와 차수 점수·동점자 기준 */ }
    majorAssignment { /* 학과 배정 규칙 */ }
    waitlist { /* 예비합격 */ }
    additionalRecruitment { /* 추가모집 */ }
    schedule { /* 전형 일정 */ }
}
```

## 블록별 읽는 법

### majors — 학과와 정원

```kotlin
majors {
    major("SW", "소프트웨어개발과", capacity = 36)
    major("IOT", "스마트IoT과", capacity = 18)
    major("AI", "인공지능(AI)과", capacity = 18)
}
```

`"SW"` 같은 코드가 엔진·DB 전반에서 학과를 가리키는 키다(합계 = 총정원 72명).

### screenings — 전형과 편입 규칙

```kotlin
screenings {
    regular("GEN", "일반전형")

    regular("SPE", "특별전형(사회통합전형)") {
        quota(8)
        unfilledGoesTo("GEN")     // 미충원 정원은 일반전형으로
        rejectedFallsTo("GEN")    // 탈락자는 일반전형에서 다시 전형(편입)
        subType("BASIC_LIVING", "기초생활수급자")
        // …
    }

    extra("EXT_VETERANS", "국가보훈대상자") {
        quota(2, capPercentOfTotal = 3)
        admitOnlyWithinFirstRoundCutline() // 1차 합격선 이내인 경우만 정원 외 선발
        overflowFallsTo("SPE")
    }
}
```

`regular`는 정원 내, `extra`는 정원 외 전형이다. `unfilledGoesTo`/`rejectedFallsTo`/`overflowFallsTo`가
전형 간 **편입(fallthrough)** 경로를 선언한다 — 엔진이 이 선언대로 미선발자를 다음 전형으로 넘긴다.

### grading — 성적 산출 규칙 (졸업구분별)

졸업예정자(`CANDIDATE`)·졸업자(`GRADUATE`)는 내신 기반(`transcript`), 검정고시(`GED`)는 수식
기반(`formula`)이다. 배점이 졸업구분마다 다르다.

```kotlin
grading {
    // 중간값 scale 5 / 결과값 scale 3, HALF_UP
    rounding(intermediateScale = 5, resultScale = 3)

    transcript(CANDIDATE) {
        generalSubjects(max = 180) {
            achievement(A to 5, B to 4, C to 3, D to 2, E to 1) // 성취도→환산점수
            semester(1, 2, points = 18)   // 반영 학기와 배점
            semester(2, 1, points = 45)
            semester(2, 2, points = 45)
            semester(3, 1, points = 72)
            missingSemester(SAME_YEAR_OTHER_SEMESTER, UPPER_YEAR, LOWER_YEAR) // 결측 학기 대체 우선순위
        }
        artsSubjects(max = 60) { achievement(A to 5, B to 4, C to 3) }
        attendance(max = 30) {
            latenessPerAbsenceDay = 3   // 지각·조퇴·결과 3회 = 결석 1회
            deductionPerAbsenceDay = 3  // 결석 1회당 감점
            zeroFromAbsenceDays = 10    // 환산 결석 10회 이상이면 0점
            missingYearDefault = 5
        }
        volunteer(maxPerYear = 10) {
            step(minHours = 7, points = 10)
            step(minHours = 6, points = 8)
            // …
            floor(2)                 // 기준 미달 시 기본점
            missingYearDefault = 2
        }
    }

    transcript(GRADUATE) { /* 반영 학기·배점이 다름: 2-1/2-2/3-1/3-2 */ }

    formula(GED) {
        subjects(minInput = 50, maxInput = 100, maxScore = 240) // 교과 = (평균−50)÷50×240
        attendanceFixed(30)
        volunteer(minInput = 40, maxInput = 100, maxScore = 30) // 봉사 = (평균−40)÷60×30
    }
}
```

> 수식은 모델에 람다로 넣지 않는다 — `RangeScaleFormula`(min/max 입력·만점) 같은 **선형 환산
> 파라미터**로 데이터화한다. 그래서 요강 개정은 숫자만 바꾸면 된다.

### rounds — 차수 점수와 동점자 기준

```kotlin
rounds {
    round("FIRST", "1차 전형(서류전형)") {
        selectByMultiplier(1.3)                              // 정원의 1.3배 선발
        sumScore(subjectScore, attendanceScore, volunteerScore) // 1차 점수 = 서류 300점
        tiebreak {
            byGeneralSubjectScore()
            bySemesters(3 to 1, 2 to 2, 2 to 1, 1 to 2)      // 동점 시 학기 성적순
            byNonSubjectScore()
        }
    }
    round("SECOND", "2차 전형(역량검사·심층면접)") {
        selectByCapacity()
        val competency = manualScore("COMPETENCY", "역량검사", max = 100) // 수동 입력 점수
        val interview = manualScore("INTERVIEW", "심층면접", max = 100)
        weightedScore(max = 100) {
            part(roundScore("FIRST"), weightPercent = 50, normalizeTo = 100)
            part(competency, weightPercent = 30)
            part(interview, weightPercent = 20)
        }
        tiebreak { byManualScore("COMPETENCY"); /* … */ }
    }
}
```

`manualScore("COMPETENCY"/"INTERVIEW")`가 곧 엔진 입력의 `RoundApplicant.manualScores` 키다
([engine.md](./engine.md) 참고). 2차 점수는 1차 점수 50% + 역량 30% + 면접 20%.

### 나머지 블록

```kotlin
majorAssignment {
    choiceCount = 3               // 지망 학과 수(1~3지망)
    extraScreeningCapPerMajor = 2 // 정원 외 전형은 학과당 최대 2명
}
waitlist { percentOfTotal(3); from("GEN") }          // 예비합격
additionalRecruitment { screening("GEN"); basedOnRound("FIRST") } // 추가모집
schedule { event("APPLICATION", "원서 접수…", LocalDate.of(2025, 10, 20), …) } // 일정
```

## 새 학년도 요강 추가하기

1. **파일을 수정하지 말고 추가한다.** `Plan2026.kt`를 복사해 `Plan2027.kt` 신규 파일로 만들고
   `val plan2027 = admissionPlan(year = 2027) { … }`로 선언한다. 과거 plan은 재현성을 위해 보존한다.
2. 요강 PDF의 수치를 그대로 옮긴다. 수치의 근거는 PDF뿐이다.
3. 요강 수치를 고정하는 테스트(`Plan2026Test` 류)를 함께 추가한다. 요강 개정 없이 이 테스트를
   고쳐 통과시키지 않는다.
4. 배치가 새 plan을 쓰게 하려면 `entrance-batch`의 엔진 빈 설정(`EngineConfig`)에서 주입하는
   plan을 바꾼다([batch.md](./batch.md)).

## 설계 규칙 (위반 금지)

- **DSL은 데이터, 엔진은 로직.** 모델에 람다/함수 타입 필드를 넣지 않는다. 규칙이 표현 안 되면
  모델을 확장하고, 위원회 재량 조항은 엔진의 수동 오버라이드로 처리한다.
- 새 모델 필드를 추가하면 **반드시 `PlanValidator` 검증 규칙도 함께** 추가한다.
- 빌더 필수 값은 nullable var + `requireNotNull`(build 시점 실패), 모델 정합성은 `PlanValidator`
  (생성 시점 실패)로 역할을 나눈다.
