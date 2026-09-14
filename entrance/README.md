# entrance — 입학전형 엔진

입학전형 요강을 **Kotlin DSL로 선언**하고, 공용 엔진이 성적 계산·선발·배정을 수행하는 모듈군입니다.
기존 `go-hellogsm`(배치)과 `go-hellogsm-score-calculator`(Lambda)를 대체합니다.

```
entrance-dsl      도메인 모델 + DSL 빌더 (순수 Kotlin, 의존성 없음)
entrance-plans    현재 활성 요강 선언 — Plan.kt (지난 연도는 legacy/에 보관)
entrance-engine   해석 엔진 — scoring·evaluation·assignment
entrance-batch    DB 러너, go-hellogsm 대체
entrance-lambda   모의 성적 계산 API, go-hellogsm-score-calculator 대체
```

## 문서

문서는 Notion에서 관리합니다 — **[입학전형 DSL](https://app.notion.com/p/team-themoment/DSL-3d43f72561858033b87ce80ac3300d24)**

| 문서 | 언제 보나 |
|---|---|
| 소개 | 처음 보는 사람용 — DSL 패러다임, 로컬에서 돌려보는 법 |
| DSL 레퍼런스 | DSL 문법 전체, 검증 규칙, 새 학년도 plan 추가법 |
| 로컬 테스트 가이드 | DB 준비 → 목데이터 → 배치 실행 → 초기화 명령어 모음 |
| 아키텍처 | "엔진은 DB를 모른다"의 설계 근거 |
| 요강을 DSL로 선언하기 | 블록별로 요강을 읽는 법, 설계 규칙 |
| 해석 엔진 | scoring·evaluation·assignment 엔진의 입출력 상세 |
| entrance-batch | 잡 전체 목록, DB↔엔진 매핑, 대조 리포트 |
| entrance-lambda | 모의 성적 계산 API의 요청/응답 계약, 배포 |
| 도메인 용어집 | 전형·차수·정원 외·동점자 등 용어와 코드 대응 |

개발 규칙(DSL 설계 원칙, `BigDecimal` 정책, plan 파일 절차)은 저장소에 남아 있습니다 —
[`.claude/skills/project-rules/references/entrance.md`](../.claude/skills/project-rules/references/entrance.md)

## 빠른 시작

```bash
./gradlew build                                             # 전체 빌드 + 테스트
./gradlew :entrance-engine:test                             # 엔진 테스트만
./gradlew :entrance-dsl:test --tests '*PlanValidatorTest*'  # 단건 테스트
./gradlew :entrance-batch:bootJar                           # 배치 실행 아티팩트
```

요강 하나는 `admissionPlan { ... }` 블록 하나로 선언합니다. 정본은
[`entrance-plans/src/main/kotlin/kr/hellogsm/entrance/plans/Plan.kt`](entrance-plans/src/main/kotlin/kr/hellogsm/entrance/plans/Plan.kt)
가 2026 요강 전문을 인코딩한 실전 레퍼런스입니다.

빌드 결과물은 불변 모델 `AdmissionPlan`이며, **생성되는 순간 정합성이 검증**됩니다. 정원 합계가 안
맞거나 가중치 합이 100%가 아니면 그 자리에서 `PlanValidationException`이 발생하므로, 존재하는 plan
인스턴스는 항상 유효합니다.

## 기타 파일

| 경로 | 용도 |
|---|---|
| [`docs/local-schema.sql`](docs/local-schema.sql) | 로컬 DB 스키마 DDL 스냅샷 — `entrance-batch`를 로컬에서 돌리기 전에 적용 |
