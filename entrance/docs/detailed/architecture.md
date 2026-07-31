# 아키텍처 — "엔진은 DB를 모른다"

entrance 모듈군의 설계 원칙 하나만 이해하면 나머지는 따라온다: **엔진은 순수한 계산기이고,
DB 입출력은 엔진을 감싸는 어댑터가 전담한다.**

## 엔진은 순수 함수다

`entrance-engine`의 세 엔진은 전부 "평범한 메모리 객체를 받아 평범한 객체를 돌려주는" 함수다.

```kotlin
EvaluationEngine.evaluate(roundCode: String, applicants: List<RoundApplicant>): RoundResult
AssignmentEngine.assign(candidates: List<FinalCandidate>): AssignmentResult
ScoringEngine.score(record: StudentRecord): ScoreBreakdown
```

입력 타입(`RoundApplicant`, `StudentRecord` 등)은 엔진/DSL이 정의한 순수 타입이지 JPA
엔티티(`Oneseo`)가 아니다. **이 입력이 DB에서 왔는지, CSV에서 왔는지, 테스트 픽스처인지
엔진은 모른다.**

`entrance-engine`의 의존성은 `entrance-dsl` 하나뿐이다:

```kotlin
// entrance/entrance-engine/build.gradle.kts
dependencies {
    api(project(":entrance-dsl")) // 순수 Kotlin. 이게 전부.
}                                 // persistence·server·JPA·Spring 없음
```

그래서 "DB를 모른다"는 비유가 아니라 **컴파일 타임 사실**이다. 엔진 클래스패스에 JPA·`Oneseo`·
repository가 아예 없어서, 누가 실수로 `import ...Oneseo` 한 줄을 넣으면 **빌드가 깨진다.**

## DB 입출력은 어댑터(entrance-batch)가 전담한다

DB를 아는 건 엔진이 아니라 엔진을 감싸는 바깥 레이어다. 이게 `entrance-batch`다.

```
 MySQL ──read(JPA)──▶┌──────────── entrance-batch (어댑터) ────────────┐
                     │  ① 매핑: Oneseo/MiddleSchoolAchievement(JPA)      │
                     │          → StudentRecord / RoundApplicant          │
                     │                       │                            │
                     │                       ▼                            │
                     │        ┌──── entrance-engine (순수) ────┐          │
                     │        │  score() / evaluate() / assign() │ ← DB 모름 │
                     │        └───────────────┬──────────────────┘          │
                     │                        │ RoundResult/AssignmentResult │
                     │                        ▼                            │
                     │  ② 매핑: 결과 → firstTestPassYn/decidedMajor(JPA)    │
 MySQL ◀──write(JPA)─┤                                                     │
                     └──────────────────────────────────────────────────────┘
```

- **①②의 "번역"**(JPA 엔티티 ↔ 엔진 타입)이 어댑터의 일이고, 그 사이의 **계산만** 엔진이 한다.
- 트랜잭션·커넥션·쿼리는 전부 어댑터 쪽. 엔진은 인메모리 리스트 in → 인메모리 결과 out.

이것이 헥사고날(포트 & 어댑터) 구조다: **엔진 = 도메인 코어(순수), 배치/서버 = 가장자리 어댑터.**

## 의존성 방향은 한 방향뿐

```
entrance-batch ──▶ entrance-engine ──▶ entrance-dsl
      │                 (순수)             (순수)
      └──▶ persistence(JPA)     ← 배치만 "두 세계"(DB·엔진)를 다 안다
```

화살표가 **배치 → 엔진 → dsl** 한 방향이다. 엔진은 배치·persistence를 역참조하지 않는다.
통합 레포가 되어도 이 규칙은 그대로다 — 모듈 그래프가 경계를 컴파일 타임에 강제한다.

## 왜 이렇게까지 하나

- **재사용**: 같은 엔진이 배치(`entrance-batch`)에서도, 향후 서버 in-process 호출에서도, 가용성
  요건용 Lambda에서도 동일하게 돈다. DB를 알면 이게 불가능하다.
- **테스트 용이성**: 엔진은 DB 없이 순수 단위 테스트로 전 범위를 검증한다(요강 수치 golden,
  기존 go-hellogsm 대비 golden). 실제로 엔진은 **테스트에서만** 도는 것으로 완결되고, 배치가
  그 위에 DB I/O만 얹는다.
- **경계 강제**: "성적 로직에 DB 코드가 섞이는" 흔한 부패를 원천 차단한다.

## 데이터냐 로직이냐 — 모듈 배치 규칙

| 이것은… | 여기에 둔다 |
|---|---|
| 배점·정원·동점자 기준·환산식 파라미터 (요강 수치) | `entrance-plans` (데이터, 로직 금지) |
| 그 수치를 해석하는 계산 규칙 | `entrance-engine` |
| DB 읽기/쓰기, 엔티티↔엔진 타입 변환, 잡 실행 | `entrance-batch` |
| 서버·배치가 공유하는 JPA 엔티티 | `persistence` |

위원회 재량 조항처럼 요강만으로 표현 안 되는 규칙은 모델에 람다를 넣지 말고, 엔진의 **수동
오버라이드 입력**으로 처리한다(예: `UnresolvedTieException` → 입학전형위원회 결정).

## 재현성·정확도 규칙

- **점수는 전부 `BigDecimal`.** Double 연산 금지(이진 오차). 반올림은 plan의 `RoundingPolicy`를
  따른다(중간값 scale 5 / 결과값 scale 3, HALF_UP).
- **연도별 plan은 수정이 아니라 추가.** 과거 배치 재현을 위해 `PlanXXXX.kt`를 보존한다.
- 요강과 기존 구현(go-hellogsm)이 다르면 **요강이 정답**이며, 차이는 문서화한다.
