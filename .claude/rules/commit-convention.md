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
| `build`   | Gradle module wiring, JVM/toolchain changes (used interchangeably with `chore` for multi-module build graph changes) | `build(entrance): 엔진 JVM target 21 → 25` |

## Scopes

| Scope            | Applies to                                          |
|-------------------|-----------------------------------------------------|
| `global`          | Shared infrastructure, config, global exception, security |
| `member`          | Member domain (entity, service, controller, DTO)    |
| `oneseo`          | Oneseo (application form) domain                   |
| `operation`       | Operation/schedule/announcement domain              |
| `common`          | Cross-cutting domain utilities (date, schedule)     |
| `persistence`     | Shared JPA module (`persistence`) used by `server` and `entrance-batch` |
| `entrance`        | Cross-cutting entrance engine changes (build wiring, docs spanning `entrance-dsl`/`entrance-plans`/`entrance-engine`) |
| `entrance-batch`  | `entrance-batch` module (DB runner, CLI jobs)        |

Within the `entrance-*` modules, commits also use narrower scopes inherited from the former standalone repo (`dsl`, `plans`, `engine`) when a change is local to one of those — see `entrance/CLAUDE.md` and `git log` for precedent.

## Multi-file Commits
- Group logically related changes in one commit
- Split unrelated changes into separate commits
- Keep each commit buildable and test-passing

## Branch Strategy (Git Flow)
- `main` — production releases only
- `develop` — integration branch
- `feature/{description}` — feature branches cut from `develop`
- `hotfix/{description}` — cut from `main`, merged to both `main` and `develop`

## Pull Request
- Target branch: `develop` (not `main`)
- PR title mirrors the commit subject: `{type}({scope}): {description}`
- PR body: Korean description of what changed and why

## Prohibited Patterns
- No `git commit --amend` on pushed commits
- No force push to `main` or `develop`
- No `--no-verify` to skip Spotless formatting hooks
- No commits with WIP code that breaks the build