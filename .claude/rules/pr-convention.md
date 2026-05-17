# PR Convention Rules — hellogsm-server-25

> PR(Pull Request) 제목/본문 규칙. **커밋 컨벤션과는 다름** — 혼동 금지.

## PR Title Format

```
[{scope}] {Korean description}
```

- **대괄호로 scope 감싸기** — `()`, `:`, `type(scope):` 등 커밋 스타일 사용 금지
- **한국어** 설명 (영문 식별자/약어는 그대로 사용 가능)
- 마침표(`.`) 로 끝내지 않음
- 제목 72자 이내
- scope는 소문자 단일 토큰

## Scope 선택 규칙

| Scope       | 사용 시점                                              |
|-------------|--------------------------------------------------------|
| `global`    | 여러 도메인에 영향, 인프라/설정/공통 모듈/CI·CD/문서   |
| `member`    | Member 도메인 단일 변경                                |
| `oneseo`    | Oneseo(원서) 도메인 단일 변경                          |
| `operation` | Operation/일정/공지 도메인 단일 변경                   |
| `common`    | 공통 도메인 유틸 (date, schedule 등) 단일 변경         |

**여러 도메인이 동시에 바뀌면 무조건 `[global]`.** 가장 비중 큰 도메인을 골라 쓰지 않는다.

## 실제 예시 (themoment-team/hellogsm-server-25 머지된 PR)

```
[global] 테스트 코드 스타일 및 명칭 표준화           # #368
[oneseo] 성취점수 리스트 내 null 요소로 인한 NPE 수정 # #366
[global] 프로젝트 이름 변경 및 아이콘 추가            # #361
[global] 커밋 해시 8자리 설정 및 push 스텝 추가       # #359
[global] Redis 캐시 직렬화 시 BigDecimal 타입 허용 추가 # #353
[oneseo] 인적사항 수정 API 추가                       # #352
```

## PR Body Format

`.github/PULL_REQUEST_TEMPLATE.md` 구조를 따른다.

```markdown
## 개요

작업 내용 1~3문장 요약

## 본문

더 자세한 작업 내용

### 추가

새 기능/엔드포인트/파일이 추가된 경우에만 포함

### 변경

기존 동작/설정/코드가 변경된 경우에만 포함
```

- `### 추가` 와 `### 변경` 은 **해당 사항이 있을 때만** 포함 (둘 다 가능, 하나만 가능)
- 한국어로 작성
- 코드 예시는 백틱 블록 사용

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
