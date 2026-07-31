# entrance 엔진 소개 — 입학전형 요강, 이제 Kotlin DSL로 선언합니다

> 이 문서는 `entrance-dsl`/`entrance-plans`/`entrance-engine`/`entrance-batch`/`entrance-lambda`
> (통칭 entrance 모듈군)를 처음 보는 팀원에게 "우리가 뭘 왜 이렇게 만들었는지"와 "로컬에서
> 직접 돌려보는 법"을 소개하기 위한 쇼케이스입니다. 레퍼런스 문서(전체 API, 상세 규칙)는
> [`docs/entrance/`](detailed/README.md)에 따로 있으니, 이 문서를 먼저 읽고 필요할 때
> 그쪽으로 넘어가면 됩니다.

정본(실제 2026 요강 인코딩)은
[`Plan.kt`](../entrance-plans/src/main/kotlin/kr/hellogsm/entrance/plans/Plan.kt),
DSL 작성법은
[`docs/about-dsl.md`](about-dsl.md),
그리고 로컬에서 직접 돌려보는 절차는 [`docs/test_guide.md`](test-guide.md)를 참고하세요.

## TL;DR

광주소프트웨어마이스터고 입학전형 로직(성적 계산 + 1차/2차 선발 + 학과 배정)이 예전엔
`server`(Java), `go-hellogsm`(배치, Go), `go-hellogsm-score-calculator`(Lambda, Go) 세 곳에
흩어져 있었습니다. 매년 요강이 바뀔 때마다 세 군데를 다 찾아 고쳐야 했고, 하나라도 빠뜨리면
합격자가 갈리는 사고로 이어질 수 있는 구조였습니다.

그래서 요강을 **Kotlin DSL로 선언한 데이터**로 만들고, 그 데이터를 해석하는 **순수 함수
엔진** 하나로 성적 계산·선발·배정을 전부 처리하도록 다시 짰습니다. 매년 바뀌는 건 이제
파일 하나(`Plan.kt`)뿐입니다.

## 1. DSL 패러다임 — "DSL은 데이터, 엔진은 로직"

핵심 원칙은 딱 하나입니다: **요강 DSL 블록의 결과물은 코드가 아니라 불변 데이터(`AdmissionPlan`)다.**

```kotlin
val plan = admissionPlan(year = 2026) {
    majors { major("SW", "소프트웨어개발과", capacity = 36) }
    screenings { regular("GEN", "일반전형") }
    grading { /* ... */ }
    rounds { /* ... */ }
}
```

이 블록을 실행하면 함수 호출이 일어나는 게 아니라, `AdmissionPlan`이라는 **평범한 객체**가
만들어집니다. 그 객체 안엔 조건문도, 반복문도, 위임도 없습니다 — 그냥 정원 숫자, 배점 숫자,
동점자 비교 순서 같은 **값**입니다. 그래서:

- **람다/함수 타입을 모델에 넣지 않습니다.** "이 경우엔 이렇게 계산해라"는 규칙이 생기면
  DSL 문법을 늘리지, plan 파일에 `if`를 쓰지 않습니다. 위원회 재량(예: "입학전형위원회에서
  심의·결정")처럼 요강만으로 표현 안 되는 조항은 애초에 DSL에 넣지 않고 엔진의 **수동 오버라이드
  입력**으로 처리합니다.
- **모든 점수는 `BigDecimal`입니다.** 요강이 반올림 자릿수를 명시하는 도메인이라, `Double`의
  이진 오차를 허용하지 않습니다. 수식도 람다가 아니라 `RangeScaleFormula` 같은 **선형 환산
  파라미터**로 선언합니다.
- **생성되는 순간 검증됩니다.** `AdmissionPlan`의 생성자가 `PlanValidator`를 거칩니다. 정원
  합계가 안 맞거나 가중치 합이 100%가 아니면 그 자리에서 `PlanValidationException`이 오류를
  모아 한 번에 던지므로, **존재하는 plan 인스턴스는 항상 유효**하다는 보장이 생깁니다.

```kotlin
val badPlan = admissionPlan(year = 2027) {
    majors { major("SW", "소프트웨어개발과", capacity = 10) }
    screenings {
        regular("GEN", "일반전형") { quota(8) }
        regular("SPE", "특별전형") { quota(5) }   // 8 + 5 > 10 !
    }
}
// PlanValidationException: 입학전형 plan 검증 실패 (1건):
//  - 정원 내 전형의 고정 정원 합(13)이 총정원(10)을 초과함
```

- **연도별 plan은 고정 이름 + legacy 보관.** 활성 plan은 항상 `entrance-plans`의
  `Plan.kt`(`val plan`)라는 고정된 이름으로 존재합니다. 소비자(`entrance-batch`,
  `entrance-lambda`)는 몇 년도 요강인지 몰라도 `kr.hellogsm.entrance.plans.plan` 하나만
  참조하면 됩니다. 학년도가 바뀌면 지금 내용을 `legacy/PlanXXXX.kt`로 얼려 옮기고 `Plan.kt`를
  새 내용으로 덮어씁니다 — 과거 plan은 지난 시즌 배치를 재현하기 위해 그대로 보존됩니다.

## 2. DSL 작성 요령 — 2026 요강이 실제로 이렇게 생겼습니다

전체 문법은 [`entrance/README.md`](about-dsl.md)의 DSL 레퍼런스에 있고, 여기서는
"요강 한 편을 어떻게 나눠 읽는지" 감을 잡는 정도로 훑습니다. 정본은
[`Plan.kt`](../entrance-plans/src/main/kotlin/kr/hellogsm/entrance/plans/Plan.kt)입니다.

**학과 · 전형** — 정원 내(`regular`)/정원 외(`extra`)를 구분하고, 전형 간 이동(fallback) 세
가지(`unfilledGoesTo`/`rejectedFallsTo`/`overflowFallsTo`)를 선언합니다.

```kotlin
majors {
    major("SW", "소프트웨어개발과", capacity = 36)
    major("IOT", "스마트IoT과", capacity = 18)
    major("AI", "인공지능(AI)과", capacity = 18)
}
screenings {
    regular("GEN", "일반전형")                 // quota 생략 = 나머지 정원 전부
    regular("SPE", "특별전형(사회통합전형)") {
        quota(8)
        unfilledGoesTo("GEN")                  // 미충원분 → 일반전형 추가 선발
        rejectedFallsTo("GEN")                 // 탈락자 → 일반전형에 편입되어 함께 전형
    }
    extra("EXT_VETERANS", "국가보훈대상자") {
        quota(2, capPercentOfTotal = 3)        // 2명, 총정원 3% 이내 상한
        admitOnlyWithinFirstRoundCutline()     // 1차 합격자 최저점 이내에서만 정원 외로
        overflowFallsTo("SPE")
    }
}
```

**성적 산출** — 졸업구분(`CANDIDATE`/`GRADUATE`/`GED`)별로 산출 스키마가 다릅니다. 반올림
정책은 plan 전체에 한 번만 선언합니다.

```kotlin
grading {
    rounding(intermediateScale = 5, resultScale = 3)   // 중간값 5자리 / 결과값 3자리, HALF_UP
    transcript(CANDIDATE) {
        generalSubjects(max = 180) {
            achievement(A to 5, B to 4, C to 3, D to 2, E to 1)
            semester(1, 2, points = 18); semester(2, 1, points = 45)
            semester(2, 2, points = 45); semester(3, 1, points = 72)
            missingSemester(SAME_YEAR_OTHER_SEMESTER, UPPER_YEAR, LOWER_YEAR)
        }
        // artsSubjects / attendance / volunteer 블록도 같은 방식
    }
    formula(GED) {                                     // 검정고시는 수식 기반
        subjects(minInput = 50, maxInput = 100, maxScore = 240)
    }
}
```

**전형 절차 · 동점자** — N차 전형을 선언 순서대로 쌓고, 동점자 비교 체인도 선언 순서가
우선순위입니다.

```kotlin
rounds {
    round("FIRST", "1차 전형(서류전형)") {
        selectByMultiplier(1.3)
        sumScore(subjectScore, attendanceScore, volunteerScore)
        tiebreak {
            byGeneralSubjectScore()
            bySemesters(3 to 1, 2 to 2, 2 to 1, 1 to 2)
            byNonSubjectScore()
        }
    }
    round("SECOND", "2차 전형(역량검사·심층면접)") {
        selectByCapacity()
        val competency = manualScore("COMPETENCY", "역량검사", max = 100)   // 운영자 수동 입력
        val interview = manualScore("INTERVIEW", "심층면접", max = 100)
        weightedScore(max = 100) {
            part(roundScore("FIRST"), weightPercent = 50, normalizeTo = 100)
            part(competency, weightPercent = 30)
            part(interview, weightPercent = 20)
        }
    }
}
```

**학과 배정 · 예비합격 · 추가모집**

```kotlin
majorAssignment { choiceCount = 3; extraScreeningCapPerMajor = 2 }
waitlist { percentOfTotal(3); from("GEN") }
additionalRecruitment { screening("GEN"); basedOnRound("FIRST") }
```

새 학년도 요강을 추가할 때 절차나 검증 규칙 전체 목록은
[`entrance/README.md`](about-dsl.md#새-학년도-요강-추가하기)를 보세요.

## 3. 한눈에 보는 아키텍처

```
entrance-plans (요강 데이터, Plan.kt)
        │
        ▼
entrance-dsl (도메인 모델 + 빌더, 순수 Kotlin)
        ▲
        │
entrance-engine (scoring → evaluation → assignment, 순수 함수)
   ▲            ▲
   │            │
entrance-batch  entrance-lambda
(DB 러너,        (모의 성적 계산 API,
 go-hellogsm 대체) go-hellogsm-score-calculator 대체)
```

가장 중요한 규칙: **`entrance-engine`은 `persistence`·`server`를 의존성으로 선언하지
않습니다.** DB를 아는 건 엔진이 아니라 엔진을 감싸는 `entrance-batch`(어댑터)뿐입니다. 그래서
같은 엔진이 배치·Lambda·(앞으로는 서버 in-process 호출)에서 전부 재사용됩니다. 자세한 설계
근거는 [`docs/entrance/architecture.md`](detailed/architecture.md) 참고.

## 4. 로컬에서 직접 돌려보기

가벼운 것부터 무거운 것 순서로 나열했습니다. 대부분은 DB나 AWS 없이 바로 돌아갑니다.

### 4.1 통짜로 빌드 + 테스트

```bash
./gradlew build   # entrance-* 전 모듈 컴파일 + 테스트
```

### 4.2 plan 하나만 유효성 확인

`Plan.kt`가 생성 시점에 검증을 통과하는지, 요강 수치가 고정 테스트와 맞는지만 봅니다.

```bash
./gradlew :entrance-plans:test --tests "*PlanTest*"
```

### 4.3 scoring 엔진 단독 실행 (순수 함수, DB·AWS 전혀 없음)

`ScoringEngine(plan).score(record)` 하나가 전부입니다. Go 계산기 대비 parity golden test도
같이 있습니다.

```bash
./gradlew :entrance-engine:test --tests "*ScoringEngineTest*"
./gradlew :entrance-engine:test --tests "*GoParityGoldenTest*"     # Go 대비 108케이스
```

### 4.4 evaluation · assignment 엔진 (여전히 DB 없음)

1차/2차 선발, 동점자, 학과 배정, 예비합격, 추가모집 — go-hellogsm 대비 366명 parity 포함.

```bash
./gradlew :entrance-engine:test --tests "*EvaluationEngineTest*"
./gradlew :entrance-engine:test --tests "*AssignmentEngineTest*"
./gradlew :entrance-engine:test --tests "*BatchParityGoldenTest*"
```

### 4.5 entrance-lambda를 AWS 없이 직접 호출해보기

`ScoreCalculatorHandler`는 API Gateway 이벤트 객체 하나만 로컬에서 만들어 주면 됩니다 — Lambda
에 배포하지 않아도 실제 서버가 받는 것과 똑같은 JSON 요청/응답을 그대로 확인할 수 있습니다.

```bash
./gradlew :entrance-lambda:test --tests "*ScoreCalculatorHandlerTest*"
```

### 4.6 entrance-batch — 목데이터 생성 (예전 "DML"의 자리)

예전 `go-hellogsm-ops`의 `generate-dml`은 SQL insert문 **파일**을 만드는 도구였습니다.
`entrance-batch`는 이를 `seed-testdata` 잡으로 대체했는데, 파일을 생성하는 대신 엔티티를
만들어 **바로 DB에 insert**합니다.

```bash
export BATCH_DB_URL="jdbc:mysql://localhost:3306/hellogsm"
export BATCH_DB_USERNAME=root
export BATCH_DB_PASSWORD=

./gradlew :entrance-batch:bootRun --args="--job=seed-testdata --screening=GEN10,SPE2,EXT1 --status=FIRST --graduate=RANDOM"
```

- `--screening`(필수): `GEN`/`SPE`/`EXT` 뒤에 인원수, `,`로 구분 (`EXT`는 국가보훈/특례 중 무작위)
- `--status`(필수): `FIRST`/`SECOND`/`FINAL_MAJOR`/`RE_EVALUATE` — 어느 배치 단계 직전 상태로 만들지
- `--graduate`(선택, 기본 `RANDOM`): `CANDIDATE`/`GRADUATE`/`GED`/`RANDOM`
- `BATCH_DB_URL`이 실수로 stage/prod를 가리키고 있지 않은지 실행 전에 직접 확인하세요 — 대화형
  confirm 없이 바로 DB에 insert합니다.

### 4.7 entrance-batch — 실제 배치 잡 실행

```bash
./gradlew :entrance-batch:bootRun --args="--job=status"                  # 접수 대상 원서 수 확인
./gradlew :entrance-batch:bootRun --args="--job=first-eval --dry-run"    # 저장 없이 리포트만
./gradlew :entrance-batch:bootRun --args="--job=first-eval"              # 1차 전형 확정
./gradlew :entrance-batch:bootRun --args="--job=second-eval"             # 2차 전형 확정
./gradlew :entrance-batch:bootRun --args="--job=assign"                  # 최종 학과 배정
```

`second-eval`은 `TestResult.competencyEvaluationScore`/`interviewScore`가 DB에 미리 채워져
있어야 합니다(운영자 입력 API가 채우는 값이라 배치가 만들지 않습니다).

### 4.8 통합 테스트로 전체 흐름 한 번에 (Docker만 있으면 DB 세팅 불필요)

로더 → 파이프라인 → 잡 → DB 기록까지, `entrance-batch`가 실제로 도는 걸 가장 빠르게 보는
방법입니다. Testcontainers가 임시 MySQL을 알아서 띄웁니다.

```bash
./gradlew :entrance-batch:test --tests "*SeedTestDataJobIntegrationTest*"
./gradlew :entrance-batch:test --tests "*FirstEvaluationJobIntegrationTest*"
```

## 5. 더 읽어보기

| 문서 | 언제 보나 |
|---|---|
| [`entrance/README.md`](about-dsl.md) | DSL 문법 전체 레퍼런스, 새 학년도 plan 추가법 |
| [`docs/entrance/architecture.md`](detailed/architecture.md) | "엔진은 DB를 모른다"의 설계 근거 |
| [`docs/entrance/engine.md`](detailed/engine.md) | scoring·evaluation·assignment 엔진의 입출력 상세 |
| [`docs/entrance/batch.md`](detailed/batch.md) | `entrance-batch` 잡 전체 목록, DB↔엔진 매핑 |
| [`docs/entrance/glossary.md`](detailed/glossary.md) | 전형·학과·동점자 등 도메인 용어집 |
| [`.claude/rules/entrance.md`](../../.claude/rules/entrance.md) | 개발 규칙 (DSL 설계 원칙, BigDecimal 정책, plan 파일 절차) |
