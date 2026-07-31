# 로컬 테스트 가이드

> `entrance-batch`로 입학전형 배치를 로컬에서 굴려보기 위한 명령어 모음집입니다. "DB 준비 →
> 목데이터 생성 → 배치 실행 → 결과 확인 → (필요하면) Plan 수정 후 재검증 → 초기화하고 재시도"
> 흐름을 순서대로 다룹니다. 개념 설명(DSL, 엔진 구조)은
> [`docs/introduce.md`](introduce.md)를, 잡 자체의 상세 동작은
> [`docs/entrance/batch.md`](detailed/batch.md)를 참고하세요.

## 0. 사전 준비물

| 필요한 것 | 용도 |
|---|---|
| JDK 25 | 전체 모듈 빌드 |
| 로컬 MySQL 8 (또는 Docker) | `entrance-batch`가 읽고 쓰는 대상 |
| Docker | `entrance-batch:test`의 Testcontainers 통합 테스트용 (수동 DB 세팅 없이 전체 흐름을 보는 가장 빠른 방법 — 아래 6번 참고) |

`entrance-batch`는 **스키마를 만들지 않습니다**(`ddl-auto: none`, [`docs/entrance/batch.md`](detailed/batch.md) 참고). 테이블은 `server`(Spring Boot)가 관리하므로, 로컬 DB에 테이블이 없다면 먼저 DDL 스냅샷([`docs/entrance/local-schema.sql`](detailed/local-schema.sql))을 적용해 스키마를 만들어야 합니다(아래 1.2).

## 1. 로컬 DB 세팅

### 1.1 MySQL 준비

```bash
# Docker로 가볍게 띄우는 경우
docker run -d --name hellogsm-mysql \
  -e MYSQL_ROOT_PASSWORD= \
  -e MYSQL_ALLOW_EMPTY_PASSWORD=yes \
  -e MYSQL_DATABASE=hellogsm \
  -p 3306:3306 mysql:8.0
```

이미 로컬에 MySQL이 있다면 `hellogsm` 데이터베이스만 만들면 됩니다: `CREATE DATABASE hellogsm;`

### 1.2 스키마 생성 — DDL 스크립트 적용

`server`를 매번 띄워 스키마를 만들 필요 없이, JPA 엔티티 기준으로 미리 떠 둔 DDL 스냅샷
([`docs/entrance/local-schema.sql`](detailed/local-schema.sql))을 바로 적용하면 됩니다.
`achievement_1_1` 등 최신 컬럼까지 이미 반영되어 있습니다.

```bash
mysql -uroot -h127.0.0.1 -P3306 hellogsm < docs/entrance/local-schema.sql
```

`entrance-batch`는 이 스키마를 그대로 재사용합니다(자체 `spring.datasource.*`만 별도 지정,
`BATCH_DB_URL` 등 — 아래 2번 참고).

> **스냅샷이 오래됐다고 의심되면**(엔티티에 컬럼/테이블이 추가됐는데 이 파일에 없다면),
> `local-schema.sql` 상단 주석에 적힌 절차대로 `HIBERNATE_DDL_AUTO=create`로 `server`를 로컬
> MySQL에 한 번 띄운 뒤 `mysqldump --no-data`로 다시 떠서 파일을 갱신하세요.

## 2. entrance-batch용 DB 접속 정보

`entrance-batch`는 `server`와 별개로 `BATCH_DB_*` 환경변수를 씁니다(기본값이 이미
`localhost:3306/hellogsm` / `root` / 빈 비밀번호라 로컬 기본 세팅이면 생략 가능):

```bash
export BATCH_DB_URL="jdbc:mysql://localhost:3306/hellogsm"
export BATCH_DB_USERNAME=root
export BATCH_DB_PASSWORD=
```

## 3. 테스트 데이터 생성 (`seed-testdata`)

```bash
./gradlew :entrance-batch:bootRun --args="--job=seed-testdata --screening=GEN10,SPE5,EXT2 --status=FIRST --graduate=RANDOM"
```

- `--screening`(필수): `GEN`/`SPE`/`EXT` 뒤에 인원수, `,`로 구분(`EXT`는 국가보훈/특례 중 무작위)
- `--status`(필수): `FIRST`/`SECOND`/`FINAL_MAJOR`/`RE_EVALUATE` — 어느 배치 단계 직전 상태로 만들지
- `--graduate`(선택, 기본 `RANDOM`): `CANDIDATE`/`GRADUATE`/`GED`/`RANDOM`
- 대화형 confirm 없이 바로 DB에 insert합니다 — `BATCH_DB_URL`이 실수로 stage/prod를
  가리키고 있지 않은지 실행 전에 직접 확인하세요.
- 먼저 `--dry-run`으로 생성될 인원 수만 확인하고 싶다면:

```bash
./gradlew :entrance-batch:bootRun --args="--job=seed-testdata --screening=GEN10,SPE5,EXT2 --status=FIRST --dry-run"
```

`SECOND` 단계로 시드하면 `firstTestPassYn=YES`까지 채워지지만, `competencyEvaluationScore`/
`interviewScore`(역량검사·심층면접)는 운영자 입력 API가 채우는 값이라 배치가 만들지
않습니다 — `second-eval`/`assign`을 의미 있게 돌리려면 DB에서 직접 채워 넣어야 합니다.

```sql
UPDATE tb_entrance_test_result SET competency_evaluation_score = 85, interview_score = 90
WHERE oneseo_id IN (SELECT id FROM tb_oneseo WHERE ...);
```

## 4. 배치 실행

```bash
./gradlew :entrance-batch:bootRun --args="--job=status"                  # 접수 대상 원서 수 확인(연결 확인)
./gradlew :entrance-batch:bootRun --args="--job=first-eval --dry-run"    # 저장 없이 리포트만
./gradlew :entrance-batch:bootRun --args="--job=first-eval"              # 1차 전형 확정
./gradlew :entrance-batch:bootRun --args="--job=second-eval"             # 2차 전형 확정
./gradlew :entrance-batch:bootRun --args="--job=assign"                  # 최종 학과 배정
```

- **`--dry-run`을 먼저 돌려 합격자 수·대조 리포트를 확인한 뒤 실제 실행**하는 걸 권장합니다.
- 각 잡은 **멱등**합니다 — 상류 단계를 매번 재계산하므로 재실행하면 결과를 덮어씁니다. Plan을
  고치고 다시 검증할 때(5번) 그대로 재실행하면 됩니다.
- 결과 확인:

```sql
SELECT o.id, o.applied_screening, o.decided_major, o.pass_yn,
       r.first_test_pass_yn, r.second_test_pass_yn
FROM tb_oneseo o JOIN tb_entrance_test_result r ON r.oneseo_id = o.id;
```

## 5. Plan 수정 후 재빌드해서 다시 테스트하기

요강 수치(정원, 배점, 동점자 기준 등)를 바꾸는 건 항상
[`Plan.kt`](../entrance-plans/src/main/kotlin/kr/hellogsm/entrance/plans/Plan.kt) 파일
하나입니다. 절차:

```bash
# 1) Plan.kt 수정

# 2) plan 자체 검증 — 생성 시점 유효성 + 고정 수치 테스트
./gradlew :entrance-plans:test --tests "*PlanTest*"

# 3) 엔진 레벨 회귀 확인 (DB 없이, 빠름)
./gradlew :entrance-engine:test

# 4) entrance-batch 아티팩트 재빌드 (Plan.kt 변경을 반영하려면 반드시 다시 빌드해야 함 —
#    entrance-batch는 컴파일 시점에 plan을 정적 참조하므로 재빌드 없이는 이전 수치로 계속 돈다)
./gradlew :entrance-batch:bootJar

# 5) 4번(배치 실행) 다시 수행 — 같은 DB에 그대로 재실행 가능(멱등)
java -jar entrance/entrance-batch/build/libs/entrance-batch-*.jar --job=first-eval --dry-run
```

새 학년도로 통째로 넘어가는 절차(현재 `Plan.kt`를 `legacy/`로 얼리고 새 내용으로 덮어쓰기)는
[`entrance/README.md`](about-dsl.md)의 "새 학년도 요강 추가하기"를 참고하세요.

## 6. 처음부터 다시 테스트하기 위한 초기화

### 6.1 시드한 테스트 데이터만 지우기 (DB 유지)

`seed-testdata`가 만든 지원자 데이터를 지우고 3번부터 다시 하고 싶을 때. FK 때문에 자식
테이블부터 지워야 합니다(`tb_oneseo`가 나머지를 `cascade ALL + orphanRemoval`로 소유하지만,
raw SQL은 순서를 지켜야 함):

```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE tb_wanted_screening_change_history;
TRUNCATE tb_entrance_test_result;
TRUNCATE tb_entrance_test_factors_detail;
TRUNCATE tb_middle_school_achievement;
TRUNCATE tb_oneseo_privacy_detail;
TRUNCATE tb_oneseo;
TRUNCATE tb_member;   -- seed-testdata가 지원자마다 Member도 함께 만듦. 실 계정이 섞여 있으면 생략하고 DELETE로 대상만 골라 지울 것
SET FOREIGN_KEY_CHECKS = 1;
```

이후 3번(테스트 데이터 생성)부터 다시 시작하면 됩니다.

### 6.2 DB를 통째로 밀고 새로 시작하기

로컬 전용 DB라 실 데이터가 없다면 가장 간단합니다:

```sql
DROP DATABASE hellogsm; CREATE DATABASE hellogsm;
```

그 다음 1.2(스키마 재생성)부터 다시 밟습니다.

### 6.3 컨테이너 자체를 밀기 (Docker로 띄운 경우)

```bash
docker rm -f hellogsm-mysql
```

그 다음 1.1부터 다시 시작합니다.

## 7. 통합 테스트로 전체 흐름 한 번에 (수동 DB 세팅 없이)

로더 → 파이프라인 → 잡 → DB 기록까지 가장 빠르게 확인하는 방법입니다. Testcontainers가
임시 MySQL을 스키마까지 알아서 만들어 띄우므로 위의 1~6번을 전혀 할 필요가 없습니다(Docker만
있으면 됨). 매번 새 컨테이너를 쓰므로 초기화 걱정도 없습니다.

```bash
./gradlew :entrance-batch:test --tests "*SeedTestDataJobIntegrationTest*"
./gradlew :entrance-batch:test --tests "*FirstEvaluationJobIntegrationTest*"
./gradlew :entrance-batch:test                                              # entrance-batch 전체
```

## 8. 명령어 모음집 (치트시트)

```bash
# 빌드 / 테스트
./gradlew build                                                   # 전체
./gradlew :entrance-plans:test --tests "*PlanTest*"                # plan 검증
./gradlew :entrance-engine:test                                    # 엔진 회귀(DB 없음)
./gradlew :entrance-batch:test                                     # 배치 통합 테스트(Docker 필요)
./gradlew :entrance-batch:bootJar                                  # jar 재빌드(Plan 반영 시 필수)

# 로컬 스키마 생성 (DDL 스냅샷 적용)
mysql -uroot -h127.0.0.1 -P3306 hellogsm < docs/entrance/local-schema.sql

# 배치 실행 (Gradle 경유)
./gradlew :entrance-batch:bootRun --args="--job=status"
./gradlew :entrance-batch:bootRun --args="--job=seed-testdata --screening=GEN10,SPE5,EXT2 --status=FIRST"
./gradlew :entrance-batch:bootRun --args="--job=first-eval --dry-run"
./gradlew :entrance-batch:bootRun --args="--job=first-eval"
./gradlew :entrance-batch:bootRun --args="--job=second-eval"
./gradlew :entrance-batch:bootRun --args="--job=assign"

# jar 직접 실행 (Plan.kt 재빌드 후 재검증 등에 사용 — 5번 참고)
java -jar entrance/entrance-batch/build/libs/entrance-batch-*.jar --job=<job> [옵션...]
```

## 9. 더 읽어보기

| 문서 | 언제 보나 |
|---|---|
| [`docs/introduce.md`](introduce.md) | entrance 엔진 전체 소개, DSL 개념, 아키텍처 |
| [`docs/entrance/local-schema.sql`](detailed/local-schema.sql) | 로컬 DB 스키마 DDL 스냅샷(1.2에서 적용하는 파일) |
| [`docs/entrance/batch.md`](detailed/batch.md) | `entrance-batch` 잡별 상세 동작, DB↔엔진 매핑 |
| [`.claude/rules/entrance.md`](../../.claude/rules/entrance.md) | 개발 규칙 (DSL 설계 원칙, BigDecimal 정책, plan 파일 절차) |
| [`.agents/skills/migration-guide`](../../.agents/skills/migration-guide/SKILL.md) | 엔티티 컬럼 추가/삭제 시 DDL 반영 절차 |
