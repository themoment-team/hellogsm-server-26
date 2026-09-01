# 도메인 용어집

입학전형 도메인 용어와, 그것이 코드에서 어떤 타입·코드로 나타나는지 정리한다. 용어 표기는
요강을 따른다.

## 전형 절차

| 용어 | 뜻 | 코드에서 |
|---|---|---|
| 원서 | 지원자가 제출한 입학 지원 한 건 | `Oneseo`(JPA 엔티티) |
| 접수 완료 | 실물 원서가 도착해 전형 대상이 됨 | `Oneseo.realOneseoArrivedYn = YES` |
| 차수 | 전형 단계(1차·2차) | 라운드 코드 `"FIRST"`, `"SECOND"` |
| 1차 전형 | 서류(내신·출결·봉사) 기반 선발 | `EvaluationEngine.evaluate("FIRST", …)` |
| 2차 전형 | 역량검사·심층면접 기반 선발 | `EvaluationEngine.evaluate("SECOND", …)` |
| 학과 배정 | 최종 합격자를 지망 학과에 배치 | `AssignmentEngine.assign(…)` |
| 예비합격 | 결원 대비 순번 합격 후보 | `AssignmentEngine.waitlist(…)`, plan `waitlist{}` |
| 중도포기 재배정 | 등록 포기자 발생 시 예비합격자 승급·재배치 | `AssignmentEngine.reassign(…)` |
| 추가모집 | 정원 미달 시 추가 선발 | `EvaluationEngine.evaluateAdditionalRecruitment(…)` |

## 전형(screening) 종류

| 요강 용어 | plan 코드 | 엔티티 enum(`Screening`) |
|---|---|---|
| 일반전형 | `GEN` | `GENERAL` |
| 특별전형(사회통합) | `SPE` | `SPECIAL` |
| 국가보훈대상자(정원 외) | `EXT_VETERANS` | `EXTRA_VETERANS` |
| 특례입학대상자(정원 외) | `EXT_SPECIAL` | `EXTRA_ADMISSION` |

- **정원 내 / 정원 외**: `regular`(일반·특별)는 총정원 72명 안에서, `extra`(보훈·특례)는 정원
  **외**로 선발한다. 정원 외는 학과당 상한(`extraScreeningCapPerMajor = 2`)이 있다.
- **적용 전형(appliedScreening)**: 지원자가 원서에 쓴 전형(지망)에서 탈락해 다른 전형으로
  **편입(fallthrough)**되면, 최종적으로 전형된 전형이 적용 전형이다.
- **편입(fallthrough)**: 미충원·탈락·정원 외 초과 시 다른 전형으로 넘기는 것.
  `unfilledGoesTo` / `rejectedFallsTo` / `overflowFallsTo`로 선언한다.

## 학과(major)

| 학과 | plan 코드 / 엔티티 enum(`Major`) | 정원(2026) |
|---|---|---|
| 소프트웨어개발과 | `SW` | 36 |
| 스마트IoT과 | `IOT` | 18 |
| 인공지능(AI)과 | `AI` | 18 |

지망은 1~3지망(`choiceCount = 3`, `DesiredMajors.first/second/thirdDesiredMajor`).

## 성적 산출

| 용어 | 뜻 | 코드에서 |
|---|---|---|
| 졸업구분 | 졸업예정자/졸업자/검정고시 | `GraduationType.CANDIDATE / GRADUATE / GED` |
| 성취도 | 과목 등급 A~E | `Achievement`, plan에서 환산점수로(A→5…E→1) |
| 교과 / 비교과 | 교과=일반+예체능, 비교과=출결+봉사 | `ScoreBreakdown.subjectsScore / nonSubjectsScore` |
| 환산 결석 | 결석 + (지각·조퇴·결과)÷3 | 출결 점수 계산의 중간값 |
| 결측 학기/학년 | 성적·자료가 없는 학기/학년 | plan의 `missingSemester` / `missingYearDefault`로 대체 |
| 동점자 처리 | 같은 점수를 가르는 기준 | plan의 `tiebreak { … }`, `Tiebreakers` |

## 자주 나오는 타입

| 타입 | 무엇 | 모듈 |
|---|---|---|
| `AdmissionPlan` | 한 학년도 요강 전체(불변) | entrance-dsl |
| `StudentRecord` | 지원자 성적 원본(scoring 입력) | entrance-engine |
| `ScoreBreakdown` | 환산점수(scoring 출력) | entrance-engine |
| `RoundApplicant` / `RoundResult` | 차수 전형 입력 / 결과 | entrance-engine |
| `FinalCandidate` / `AssignmentResult` | 배정 입력 / 결과 | entrance-engine |
| `Oneseo`, `MiddleSchoolAchievement`, `EntranceTestResult` | 원서·성적·전형결과 JPA 엔티티 | persistence |

## 자주 나오는 값(2026 요강)

- 총정원 **72명**(SW 36 / IOT 18 / AI 18), 특별전형 정원 8명.
- 1차 선발 배수 **1.3배**, 2차 점수 = 1차 50% + 역량검사 30% + 심층면접 20%.
- 반올림: 중간값 소수 5자리 / 결과값 소수 3자리, HALF_UP.
