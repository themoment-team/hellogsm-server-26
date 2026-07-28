# 해석 엔진 (entrance-engine)

`entrance-engine`은 `AdmissionPlan`(요강)을 해석해 성적 계산 → 선발 → 배정을 수행하는 **순수
함수 엔진**이다. 세 엔진 모두 `plan`을 생성자로 받고, 같은 plan + 같은 입력이면 항상 같은 출력을
낸다(모든 연산은 `BigDecimal`).

```kotlin
val scoring = ScoringEngine(plan)
val evaluation = EvaluationEngine(plan)
val assignment = AssignmentEngine(plan)
```

## ① ScoringEngine — 성적 계산

지원자 성적 원본(`StudentRecord`)을 환산점수(`ScoreBreakdown`)로 바꾼다.

```kotlin
fun score(record: StudentRecord): ScoreBreakdown
```

- **입력 `StudentRecord`** (sealed): 
  - `Transcript` — 내신 기반. `generalAchievements: Map<SemesterRef, List<Achievement>>`(학기별
    성취도), `artsAchievements`, `attendanceByYear: Map<Int, AttendanceRecord>`, `volunteerHoursByYear`.
    자료가 없는 학기/학년은 키를 넣지 않는다 — 결측 대체는 엔진이 plan 규칙대로 처리한다.
  - `Ged` — 검정고시. `averageScore`(평균점수) 하나.
- **출력 `ScoreBreakdown`**: `totalScore`, `subjectsScore`, `attendanceScore`, `volunteerScore`,
  `nonSubjectsScore`, `transcriptDetail`(일반교과·예체능·학기별 점수 등).

`Achievement`는 성취도 A~E이며, plan의 `achievement(A to 5 …)` 선언에 따라 환산점수로 바뀐다.

## ② EvaluationEngine — 차수 전형(선발)

한 차수(1차·2차)의 지원자들을 받아 선발/탈락/불참을 가른다. 전형 간 편입과 동점자 처리가 여기서 일어난다.

```kotlin
fun evaluate(roundCode: String, applicants: List<RoundApplicant>): RoundResult
fun evaluateAdditionalRecruitment(applicants: List<RoundApplicant>, openSeats: Int): RoundResult
```

- **입력 `RoundApplicant`**: `id`, `screening`(이 차수 시작 시점의 전형 코드), `breakdown`(scoring
  결과), `manualScores`(수동 입력 점수 — 키 `"COMPETENCY"`/`"INTERVIEW"`), `previousRoundScores`
  (이전 차수 확정 점수 — 키 `"FIRST"`).
- **출력 `RoundResult`**: `entries: List<RoundEntry>` + `passed`. 각 `RoundEntry`는 `outcome`
  (`PASSED`/`REJECTED`/`ABSENT`), `appliedScreening`(편입 반영 최종 적용 전형), `roundScore`,
  `screeningPath`(편입 이력).

### 동점자 처리와 위원회 결정

동점자는 plan의 `tiebreak` 선언 순서대로 가른다(예: 일반교과 → 학기별(3-1·2-2·2-1·1-2) → 비교과).
그래도 완전 동점이 선발 경계에 걸리면 요강에 다음 기준이 없으므로 **`UnresolvedTieException`**을
던진다 — 입학전형위원회의 수동 결정이 필요하다는 신호다. 마찬가지로 결측 학기를 대체할 성적이
없는 등 자동 산출이 불가능한 경우도 예외로 위원회 대상임을 알린다.

## ③ AssignmentEngine — 학과 배정

최종 차수 합격자(`FinalCandidate`)를 성적순으로 지망 학과에 배정한다.

```kotlin
fun assign(candidates: List<FinalCandidate>): AssignmentResult
fun waitlist(rejectedCandidates: List<FinalCandidate>): List<String>       // 예비합격 순번
fun reassign(/* 중도포기 발생 */): ReassignmentResult                        // 재배정
```

- **입력 `FinalCandidate`**: `applicant`(최종 적용 전형 반영), `finalScore`(최종 차수 점수),
  `majorChoices`(지망 학과 코드, 지망 순서대로).
- **출력 `AssignmentResult`**: `assignments: List<MajorAssignment(applicantId, screening, majorCode,
  choiceRank)>`. 정원 외 전형은 학과당 상한(`extraScreeningCapPerMajor`)을 지킨다.
- `reassign`은 중도포기자만큼 예비합격자를 승급시키고, 정원 내·정원 외 빈자리를 **독립적으로**
  계산한다(`Vacancy.withinCapacity` / `extra`).

## 전형 한 사이클을 엔진으로 잇기

세 엔진을 순서대로 이으면 실제 전형 한 사이클이 된다. 이 구동 순서가 배치의 정본이며,
`entrance-engine`의 `BatchParityGoldenTest`가 기존 go-hellogsm 배치와 전수 대조로 검증한다.

```kotlin
// 1차
val first = evaluation.evaluate("FIRST", applicants)

// 2차 — 1차 합격자에게 적용 전형과 1차 점수를 이어준다
val secondInputs = first.passed.map { entry ->
    byId.getValue(entry.applicantId).copy(
        screening = entry.appliedScreening!!,
        previousRoundScores = mapOf("FIRST" to entry.roundScore!!),
    )
}
val second = evaluation.evaluate("SECOND", secondInputs)

// 학과 배정 — 2차 합격자를 최종 후보로
val finalists = second.passed.map { entry ->
    FinalCandidate(applicant = /* 적용 전형 반영 */, finalScore = entry.roundScore!!, majorChoices = …)
}
val result = assignment.assign(finalists)
```

각 지원자의 `breakdown`(1차 근거)과 `manualScores`(2차 역량·면접)는 어디서 왔든 상관없다 —
테스트에서는 픽스처가, 실제로는 `entrance-batch`가 DB에서 만들어 넣는다([batch.md](./batch.md)).

## 검증(테스트)이 완료 기준이다

엔진은 DB 없이 순수 단위 테스트로 전 범위를 검증한다.

- **요강 수치 golden** — plan 수치가 요강 PDF와 일치하는지 고정.
- **go-hellogsm 대비 golden** — 기존 구현과 산출이 같은지 전수 대조(`GoParityGoldenCases`,
  `BatchParityGoldenTest`). 기준 데이터는 `entrance-engine/src/test/resources/golden/`.

요강과 기존 구현이 다르면 **요강이 정답**이며, 확인된 차이(예: 학기 몫 중간 반올림, 검정고시
float64 오차)는 문서로 남긴다.

```bash
./gradlew :entrance-engine:test
```
