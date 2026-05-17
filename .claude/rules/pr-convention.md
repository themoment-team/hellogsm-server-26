# PR Convention Rules — hellogsm-server-25

> Pull Request title/body rules. **Distinct from commit convention** — do not conflate.

## PR Title Format

```
[{scope}] {Korean description}
```

- Wrap scope in **square brackets** — never use commit-style `()`, `:`, `type(scope):`
- Description in **Korean** (English identifiers/acronyms allowed inline)
- No trailing period
- ≤ 72 characters
- scope is a single lowercase token (`/`, `-` allowed, e.g. `ci/cd`)

## Scope Selection

Scope is **not a fixed whitelist.** Aside from two fixed scopes, scopes are discovered
dynamically from subdirectories under `src/main/java/team/themoment/hellogsmv3/domain/`.

| Fixed scope | When to use                                                     |
|-------------|-----------------------------------------------------------------|
| `global`    | Cross-domain changes, infra/config, security, response wrapper  |
| `ci/cd`     | CI/CD pipelines, Docker, GitHub Actions                         |

Domain scopes mirror directory names. As of 2026-05: `oneseo`, `member`, `common`
(sub-domains: `operation`, `date`, `utility`).

- Single-domain change → that domain's name
- Change limited to `common/operation/` → `operation`
- Cross-cutting change in `common/` itself → `common`
- **Any change spanning multiple domains → always `global`** — do not pick the "biggest" one

When new domains are added, this convention accepts their names automatically. Do not edit this rule file each time.

## Real Examples (merged PRs from this repo)

```
[global] 테스트 코드 스타일 및 명칭 표준화                # #368
[oneseo] 성취점수 리스트 내 null 요소로 인한 NPE 수정     # #366
[global] 프로젝트 이름 변경 및 아이콘 추가                # #361
[global] 커밋 해시 8자리 설정 및 push 스텝 추가           # #359
[global] Redis 캐시 직렬화 시 BigDecimal 타입 허용 추가   # #353
[oneseo] 인적사항 수정 API 추가                           # #352
```

## PR Body Format

Follow `.github/PULL_REQUEST_TEMPLATE.md`. Body content is written in Korean.

Required sections:
- `## 개요` — 1–3 sentence summary
- `## 본문` — detailed description

Optional sections (include only if applicable):
- `### 추가` — when new features/endpoints/files are added
- `### 변경` — when existing behavior/config/code is modified

Either one or both may appear. **Remove empty section headers entirely** — do not leave a header with no content.

## Target Branch

- Default: `develop`
- Only `hotfix/*` branches target `main`

## Prohibited Patterns

- ❌ `feat(oneseo): ...` — commit-style PR title
- ❌ `test(oneseo): ...` — commit-style PR title
- ❌ `[Oneseo] ...` — scope must be lowercase
- ❌ `[oneseo, member] ...` — multi-scope forbidden, use `[global]` instead
- ❌ English titles (e.g. `[oneseo] Add personal info modify API`)
- ❌ Trailing period in title
- ❌ Empty `### 추가` / `### 변경` headers with no body