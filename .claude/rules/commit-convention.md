# Commit Convention Rules — hellogsm-server-25

## Format
```
{type}({scope}): {description}
```

- **No capital letter** at start of description
- **Korean** descriptions are standard in this project
- **No period** at end of description
- Subject line ≤ 72 characters

## Types

| Type      | When to use                                         | Example                                     |
|-----------|-----------------------------------------------------|---------------------------------------------|
| `feat`    | New feature added                                   | `feat(oneseo): 원서 제출 기능 추가`          |
| `fix`     | Bug fix                                             | `fix(member): 중복 회원 삭제 조건 수정`      |
| `update`  | Enhancement or change to existing feature           | `update(global): Gradle 버전을 변경`         |
| `refactor`| Code restructuring without behavior change          | `refactor(oneseo): 서비스 레이어 분리`       |
| `test`    | Test additions or fixes                             | `test(member): CreateMemberService 테스트 추가` |
| `ci/cd`   | CI/CD pipeline, Docker, deployment config           | `ci/cd(global): Docker에서 Java 버전 변경`   |
| `code`    | Code quality cleanup, dead code removal             | `code(refactor): 미사용 코드 정리`           |
| `docs`    | Documentation only changes                         | `docs(global): CLAUDE.md 규칙 섹션 추가`    |
| `chore`   | Build config, dependency updates                    | `chore(global): AWS BOM 버전 변경`           |

## Scopes

| Scope       | Applies to                                          |
|-------------|-----------------------------------------------------|
| `global`    | Shared infrastructure, config, global exception, security |
| `member`    | Member domain (entity, service, controller, DTO)    |
| `oneseo`    | Oneseo (application form) domain                   |
| `operation` | Operation/schedule/announcement domain              |
| `common`    | Cross-cutting domain utilities (date, schedule)     |

## Multi-file Commits
- Group logically related changes in one commit
- Split unrelated changes into separate commits
- Keep each commit buildable and test-passing

## Branch Strategy (Git Flow)
- `main` — production releases only
- `develop` — integration branch
- `{type}/{description}` — work branches cut from `develop`
  - `{type}`은 commit type과 동일한 단일 토큰: `feature`, `fix`, `update`, `refactor`, `add`, `chore`, `docs`, `test`, `code`, `ci/cd`
  - `{description}` 은 kebab-case 영문 또는 짧은 식별자
  - **이중 type 토큰 금지** — `feature/update/...`, `feature/fix/...` 같은 형태는 사용하지 않는다. `update/...`, `fix/...` 처럼 하나만 쓴다.
- `hotfix/{description}` — cut from `main`, merged to both `main` and `develop`

### 예시
- ✅ `update/write-pr-convention`
- ✅ `fix/null-achievement-validation`
- ✅ `add/oneseo-status-filter`
- ❌ `feature/update/write-pr-convention` — `feature` + `update` 이중 토큰
- ❌ `feature/fix/null-achievement-validation` — `feature` + `fix` 이중 토큰

## Pull Request
- Target branch: `develop` (not `main`)
- PR title mirrors the commit subject: `{type}({scope}): {description}`
- PR body: Korean description of what changed and why

## Prohibited Patterns
- No `git commit --amend` on pushed commits
- No force push to `main` or `develop`
- No `--no-verify` to skip Spotless formatting hooks
- No commits with WIP code that breaks the build