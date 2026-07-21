# CLAUDE.md — hellogsm 프로덕트 전체 개요

> Hello, GSM | 광주소프트웨어마이스터고등학교 입학지원시스템 (www.hellogsm.kr)

이 문서는 `hellogsm` 프로덕트를 구성하는 5개 저장소를 아우르는 최상위 컨텍스트입니다. 각 저장소 내부의 코딩 컨벤션/규칙은 해당 저장소의 `CLAUDE.md` 및 `.claude/rules/*` (현재는 `hellogsm-server-26`에만 존재)를 우선 참고하세요. 이 문서는 **저장소 간 관계, 데이터 흐름, 도메인 지식**을 다룹니다.

## 저장소 구성

| 저장소 | 역할 | 스택 |
|---|---|---|
| `hellogsm-server-26` | 메인 API 서버 (회원/원서 도메인) | Spring Boot 4, Java 25, JPA+QueryDSL, MySQL, Redis |
| `hellogsm-client-26` | 프론트엔드 모노레포 (지원자용 client + 운영자용 admin) | Next.js 14, Turborepo, pnpm |
| `go-hellogsm` | 입학 전형 배치 잡 (1차/2차 평가, 최종 학과 배정) | Go, gorm(MyBatis 스타일) |
| `go-hellogsm-score-calculator` | 내신 성적 계산 엔진 (AWS Lambda) | Go |
| `go-hellogsm-ops` | 운영 보조 도구 (Discord 릴레이, 테스트 데이터 생성) | Go |

## 저장소 간 데이터 흐름 (중요)

```
                     ┌─────────────────────┐
   FE (client/admin) │  hellogsm-server-26  │
   ─────────────────▶│   (Spring Boot API)  │
   (모의성적 계산도    └──────────┬───────────┘
    server 통해 호출)             │ 점수 계산 호출
                                  ▼
                     ┌─────────────────────────────┐
                     │ go-hellogsm-score-calculator │
                     │   (AWS Lambda, REST API)     │
                     └──────────────┬───────────────┘
                                    │ 계산 결과를
                                    ▼
                     ┌─────────────────────┐
                     │   MySQL (공유 DB)    │◀──────────────┐
                     └──────────┬───────────┘                │
                                │ 사전 계산된 점수 조회         │
                                ▼                             │
                     ┌─────────────────────┐                 │
                     │     go-hellogsm      │─────────────────┘
                     │  (배치: 1차/2차평가,   │  전형 배치 실행
                     │   최종 학과 배정)      │
                     └─────────────────────┘
```

- **score-calculator를 별도 Lambda로 분리한 이유**: server가 다운되어 있어도 FE(client)에서 "모의 성적 계산" 기능이 동작해야 하기 때문. server가 살아있을 때는 server가 이 Lambda를 호출해 점수를 계산하고 **DB에 미리 저장**해 둔다.
- **go-hellogsm 배치는 Lambda를 직접 호출하지 않는다.** 배치가 도는 시점에는 이미 server가 계산해 DB에 저장해 둔 점수를 그대로 사용한다. 즉 실제 전형 배치에 쓰이는 점수의 계산 시점과 배치 실행 시점은 분리되어 있다.
- `go-hellogsm`과 `hellogsm-server-26`은 **같은 MySQL을 공유**한다. (마이그레이션 소유권 등 세부 정책은 확인 필요 시 팀에 문의)

## 도메인 지식 — 입학 전형 프로세스

전체 흐름: **원서(oneseo) 접수 → 1차 평가(firstEvaluationJob) → 2차 평가(secondEvaluationJob) → 최종 학과 배정(majorAssignmentJob) → (필요시) 추가 모집(RE_EVALUATE)**

### 전형 유형 (screening type)
- `GEN` — 일반전형
- `SPE` — 사회통합전형
- `EXT` — 정원외특별전형 (국가보훈대상자 `EXTRA_ADMISSION` 또는 특례입학대상자 `EXTRA_VETERANS` 중 하나로 세분)

### 졸업 상태 (graduationType)
- `CANDIDATE` — 재학생 (졸업예정, 1~3학년 성적 모두 필요)
- `GRADUATE` — 졸업자 (1~3학년 성적 모두 필요)
- `GED` — 검정고시 (평균 점수만 필요, 별도 환산식 적용)

### 배치 상태 (원서 상태, go-hellogsm-ops의 generate-dml 기준)
- `FIRST` — 1차 배치 전 base data
- `SECOND` — 2차 배치 전 base data
- `FINAL_MAJOR` — 최종 학과 배정 배치 전 base data
- `RE_EVALUATE` — 추가 모집 배치 전 base data

### 성적 계산 (go-hellogsm-score-calculator)
- 총점 300점 = 교과 성적 240점(일반교과 180점 + 예체능 60점) + 비교과 60점(출결 30점 + 봉사 30점)
- 일반교과 학기별 배점은 `CANDIDATE`/`GRADUATE`에 따라 다름 (졸업생은 3학년 배점이 재학생보다 균등 분배됨 — 상세는 해당 레포 README 참고)
- 검정고시(GED)는 학기별 성적 대신 `(평균점수-60)/40×240` 환산식 사용, 비교과는 만점 고정

### 사용자 / 권한
- 지원자는 `hellogsm-client-26`의 `apps/client`(포트 3000), 운영자(학교 행정실 등)는 `apps/admin`(포트 3001)을 사용.
- 인증은 Google/Kakao OAuth 둘 다 지원하며 **앱별로 로그인 수단이 갈리지 않는다.** 동일한 OAuth 계정 풀을 두 앱이 공유하고, **Role 기반**으로 지원자/운영자 권한을 구분한다. (즉 "어떤 provider로 로그인했는가"가 아니라 "어떤 Role을 가졌는가"로 접근 제어)

## 배포 파이프라인

| 저장소 | 트리거 | 대상 |
|---|---|---|
| `hellogsm-server-26` | PR을 수동 승인 후 `develop`/`main`에 머지(push) | 머지 즉시 GitHub Actions가 자동으로 stage/prod CD 실행 (AWS CodeDeploy, EC2) |
| `hellogsm-client-26` | 동일하게 PR 승인 후 머지 시 자동 배포. 단, `develop → main` 승격은 **매월 1일/15일 자동 생성되는 정기 배포 PR**(cron)로 이루어짐 — 승인만 사람이 함 | Discord 웹훅으로 CI/배포 성공·실패 알림 |
| `go-hellogsm`, `go-hellogsm-ops`, `go-hellogsm-score-calculator` | **CI/CD 워크플로우 없음** — 빌드/배포는 수동 (score-calculator는 `build.sh`/`build.ps1`로 Lambda용 바이너리 빌드 후 수동 업로드로 추정) | - |

- server: PR merge → `main`이면 prod CD, `develop`이면 stage CD가 자동 트리거 (`hellogsm-prod-cd.yml` / `hellogsm-stage-cd.yml`, `appspec.yml` 통한 CodeDeploy)
- Git Flow 공통: `main`(운영), `develop`(통합), `feature/*`, `hotfix/*` — server의 `.claude/rules/commit-convention.md`에 상세 규칙 있음. client/go 레포도 동일한 브랜치 전략을 따르는 것으로 보이나 커밋 컨벤션이 문서화되어 있지 않다면 server 규칙을 참고.

## 저장소별 추가 참고

- **hellogsm-server-26**: 상세 코딩/테스트/커밋/API 컨벤션은 `.claude/rules/*.md` 참고. `.agents/skills/`에 commit, test, code-review, migration-guide 등 스킬 존재.
- **hellogsm-client-26**: `apps/{client,admin}` + `packages/{api,constants,hooks,store,tailwind-config,types,typescript-config,ui,utils}` workspace 구조. 공통 로직은 `packages/*`로 분리.
- **go-hellogsm**: panic은 즉시 셧다운이 필요한 경우에만, 그 외엔 error 리턴. gorm을 ORM이 아닌 SQL mapper처럼 사용(의도적 선택).
- **go-hellogsm-ops**: `relay-api`(운영 알림 Discord 웹훅 릴레이), `generate-dml`(로컬 개발/통합테스트용 mock 데이터 생성 — **운영 환경에서 사용 금지**).
- **go-hellogsm-score-calculator**: AWS Lambda 배포, REST 형태 요청/응답. 에러 코드: `EMPTY_BODY`, `INVALID_JSON`, `VALIDATION_ERROR`, `MARSHAL_ERROR`.

## 문서화 범위 밖

다음 항목은 팀 배경/현재 진행 상황에 관한 것으로 이 문서에서 의도적으로 다루지 않음:
- 팀 조직 구조 및 연도별 저장소 운영 방식(`-25`, `-26` 등)
- 현재 진행 중인 마이그레이션이나 우선순위
