# entrance-batch — DB 러너 (사용법)

`entrance-batch`는 공유 MySQL을 읽어 엔진을 돌리고 결과를 다시 써 넣는 **Spring Boot CLI**다.
기존 **`go-hellogsm` 배치와 Lambda 성적 계산기를 대체**한다. 엔진(순수)·요강(`plan2026`)·공유
영속성(`persistence`)을 잇는 어댑터이며, 아키텍처상 위치는 [architecture.md](./architecture.md) 참고.

## 무엇을 하나

`Oneseo`(원서)와 그 성적/전형 데이터를 읽어 →
1. **성적 재계산**(`ScoringEngine`) → 2. **1차 평가** → 3. **2차 평가** → 4. **학과 배정**
을 수행하고, 결과를 DB에 기록한다.

| 잡(`--job`) | 하는 일 | DB에 쓰는 것 |
|---|---|---|
| `status` | 접수 대상 원서 수 출력(연결 확인) | — |
| `first-eval` | 성적 재계산 + 1차 전형 | `EntranceTestResult.firstTestPassYn`, `Oneseo.appliedScreening` |
| `second-eval` | 1차 + 2차 전형 | `EntranceTestResult.secondTestPassYn` |
| `assign` | 1차 + 2차 + 학과 배정 | `Oneseo.decidedMajor`, `Oneseo.passYn` |

- **대상 지원자**: `realOneseoArrivedYn = YES`(실물 원서 도착)인 원서.
- 각 잡은 **멱등**하다 — 상류 단계를 매번 재계산하므로 재실행하면 결과를 덮어쓴다. 2차/배정은
  역량검사·심층면접 점수가 DB에 입력된 뒤 실행해야 의미가 있다(그 전엔 미응시 처리된다).

## 실행법

### 1. 아티팩트 빌드

```bash
./gradlew :entrance-batch:bootJar
# → entrance/entrance-batch/build/libs/entrance-batch-*.jar
```

### 2. DB 접속 정보(환경변수)

```bash
export BATCH_DB_URL="jdbc:mysql://<host>:3306/<db>"
export BATCH_DB_USERNAME="<user>"
export BATCH_DB_PASSWORD="<password>"
```

기본값은 `jdbc:mysql://localhost:3306/hellogsm`이다. 배치는 **스키마를 건드리지 않는다**
(`ddl-auto=none`) — 테이블은 서버가 관리한다.

### 3. 잡 실행

```bash
# 연결 확인
java -jar entrance-batch-*.jar --job=status

# 1차 전형 (실제 기록)
java -jar entrance-batch-*.jar --job=first-eval

# 미리보기 — 계산·리포트만, DB 미저장
java -jar entrance-batch-*.jar --job=first-eval --dry-run

# 2차 전형 / 학과 배정
java -jar entrance-batch-*.jar --job=second-eval
java -jar entrance-batch-*.jar --job=assign
```

> **`--dry-run`을 먼저 돌려 대조 리포트와 합격자 수를 확인한 뒤 실제 실행**하는 것을 권장한다.

## 내부 구성 (동작 방식)

```
BatchOneseoRepository (접수 원서 조회)
        │
        ▼
ApplicantLoader ──▶ StudentRecordMapper ──▶ ScoringEngine  (성적 재계산 → ScoreBreakdown)
        │           (MiddleSchoolAchievement → StudentRecord)
        ▼
   RoundApplicant  (+ 역량/면접 manualScores, 지망 학과 choices, 원본 엔티티 핸들)
        │
        ▼
EvaluationPipeline ──▶ evaluate("FIRST") → evaluate("SECOND") → assign()
        │
        ▼
   각 Job이 결과를 엔티티 mutator로 write-back (@Transactional)
```

- **`StudentRecordMapper`**: `MiddleSchoolAchievement`(JPA) → `StudentRecord`(엔진). go 계산기
  (parity 기준 커밋 `d7b65b4`)의 입력 해석을 그대로 재현한다 — 성취도 5→A…1→E(0=미수강 제외),
  출결 `absentDays`+`attendanceDays`(지각·조퇴·결과÷3), 봉사, 검정고시 `gedAvgScore`.
- **`CodeMapping`**: 전형·학과의 엔티티 enum ↔ plan 코드 변환(예: `Screening.GENERAL` ↔ `"GEN"`,
  `Major.SW` ↔ `"SW"`).
- **결과 write-back**: `EntranceTestResult.decideFirstTestResult/decideSecondTestResult`,
  `Oneseo.applyScreening/decideAdmission`. 서버는 이 필드들을 **읽기만** 하고, 쓰는 주체는 배치다.

## 대조 리포트 (score reconciliation)

배치는 성적을 **재계산해 정답으로 쓰되**, DB에 저장된 `documentEvaluationScore`(Lambda 산출값)와
대조해 **허용오차(0.001) 밖 불일치만** 리포트한다. 요강 vs 기존 Go 산출에는 이미 알려진 미세
차이(학기 몫 중간 반올림 ±0.001, 검정고시 float64 오차)가 있으므로 실패로 처리하지 않고, 검토
대상만 지원자별로 출력한다. 저장값을 덮어쓰지는 않는다.

## 개발 / 테스트

- `entrance-batch` 테스트는 **Testcontainers MySQL**로 실제 DB에 지원자를 시드하고 잡을 돌려
  write-back까지 검증한다(로컬에 Docker 필요).

```bash
./gradlew :entrance-batch:test
```

- go 원본은 대조·이식 근거로 `entrance/.reference/`에 clone해 둔다(**git 추적 제외**).
  scoring parity 기준은 `go-hellogsm-score-calculator@d7b65b4`, eval/assign parity 기준은
  `go-hellogsm`의 2026 시즌 상수(정원 72, 커밋 `da09df4` 이전)다.

## 아직 없는 것

- 예비합격(`waitlist`)·중도포기 재배정(`reassign`)·추가모집(`additionalRecruitment`) 서브커맨드
  — 엔진 기능은 이미 있으므로 잡만 추가하면 된다.
- 대규모 batch_parity golden 전수 통합 테스트(엔진 레벨은 이미 검증됨).
- 실 배치 병행 운전(기존 go-hellogsm과 결과 비교).
