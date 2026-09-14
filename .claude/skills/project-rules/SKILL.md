---
name: project-rules
description: Coding, logging, exception, testing, API, infrastructure, and commit conventions for this repository. Read the matching reference before writing, changing, or committing code, Pulumi IaC, or CI/CD workflows.
---

# Project Rules

Read only the reference that matches the task — not all of them.

| Scope | Reference |
|-------|-----------|
| Java/Spring (`server`, `persistence`) coding style | `references/coding-style.md` |
| Logging | `references/logging.md` |
| Exception handling | `references/exception-handling.md` |
| Testing | `references/testing.md` |
| HTTP API conventions (`server`) | `references/api-convention.md` |
| Commit / branch / PR conventions | `references/commit-convention.md` |
| Kotlin `entrance-*` modules | `references/entrance.md` |
| Pulumi IaC (`infra/`), AWS resources, deploy workflows | `references/infra.md` |

`entrance-*` follows `references/entrance.md` instead of the Java/Spring rules.

`infra/` is a Pulumi TypeScript project, not a Gradle module — none of the Java/Kotlin rules apply to it. Read `references/infra.md` before touching `infra/**` or `.github/workflows/**`.
