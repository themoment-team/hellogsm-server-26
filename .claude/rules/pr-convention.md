# PR Convention Rules — hellogsm-server-25

> PR(Pull Request) 제목/본문 규칙. **커밋 컨벤션과는 다름** — 혼동 금지.

## PR Title Format

```
[{scope}] {Korean description}
```

- **대괄호로 scope 감싸기** — `()`, `:`, `type(scope):` 등 커밋 스타일 사용 금지
- **한국어** 설명 (영문 식별자/약어는 그대로 사용 가능)
- 마침표(`.`)로 끝내지 않음
- 제목 72자 이내
- scope는 소문자 단일 토큰 (`/`, `-` 허용 — 예: `ci/cd`)

## Scope 선택 규칙

scope는 **고정 화이트리스트가 아니다.** 두 가지 고정 scope를 제외하면, scope는
`src/main/java/team/themoment/hellogsmv3/domain/` 하위 디렉토리에서 동적으로 발견한다.

| 고정 scope | 사용 시점                                                       |
|-----------|------------------------------------------------------------------|
| `global`  | 여러 도메인에 걸친 변경, 인프라/설정/공통 모듈, 보안, 응답 래퍼 |
| `ci/cd`   | CI/CD 파이프라인, Docker, GitHub Actions                         |

도메인 scope는 디렉토리 이름을 그대로 사용한다. 2026-05 기준 디렉토리:
`oneseo`, `member`, `common` (하위: `operation`, `date`, `utility`).

- 단일 도메인 변경 → 해당 도메인 이름
- `common/operation/` 만 변경 → `operation`
- `common/` 자체가 영향 받는 cross-cutting → `common`
- **여러 도메인이 동시에 바뀌면 무조건 `global`** — 가장 비중 큰 도메인을 골라 쓰지 않는다

새 도메인이 추가되면 PR 컨벤션도 자연히 그 이름을 scope로 인정한다. 룰 파일을 매번 갱신하지 않는다.

## 실제 예시 (themoment-team/hellogsm-server-25 머지된 PR)

```
[global] 테스트 코드 스타일 및 명칭 표준화                # #368
[oneseo] 성취점수 리스트 내 null 요소로 인한 NPE 수정     # #366
[global] 프로젝트 이름 변경 및 아이콘 추가                # #361
[global] 커밋 해시 8자리 설정 및 push 스텝 추가           # #359
[global] Redis 캐시 직렬화 시 BigDecimal 타입 허용 추가   # #353
[oneseo] 인적사항 수정 API 추가                           # #352
```

## PR Body Format

`.github/PULL_REQUEST_TEMPLATE.md` 구조를 따른다. 한국어로 작성.

필수 섹션:
- `## 개요` — 1~3문장 요약
- `## 본문` — 자세한 작업 내용

선택 섹션 (해당 사항 있을 때만 추가):
- `### 추가` — 새 기능/엔드포인트/파일이 추가된 경우
- `### 변경` — 기존 동작/설정/코드가 변경된 경우

둘 중 하나만 있어도 되고, 둘 다 있어도 된다. 해당 사항이 없는 섹션은 **헤더째로 제거한다** (빈 섹션을 남기지 않음).

## Target Branch

- 기본: `develop`
- `hotfix/*` 브랜치만 `main` 대상

## Prohibited Patterns

- ❌ `feat(oneseo): ...` — 커밋 스타일 PR 제목
- ❌ `test(oneseo): ...` — 커밋 스타일 PR 제목
- ❌ `[Oneseo] ...` — scope는 소문자
- ❌ `[oneseo, member] ...` — 다중 scope 금지, 대신 `[global]`
- ❌ 영문 제목 (예: `[oneseo] Add personal info modify API`)
- ❌ 제목 끝에 마침표
- ❌ 빈 `### 추가` / `### 변경` 헤더만 남기기