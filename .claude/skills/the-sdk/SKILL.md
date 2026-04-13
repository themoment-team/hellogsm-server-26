---
description: Guide for using the-sdk common library — CommonApiResponse, ExpectedException, HTTP logging, and SDK configuration
---

# the-sdk Usage Guide

`com.github.themoment-team:the-sdk:1.5` is the shared infrastructure library for this project.
It provides HTTP logging, response wrapping, exception handling, and Swagger auto-configuration out of the box.

## When to use this skill

Invoke `/the-sdk` when you need to:
- Use `CommonApiResponse` in controllers or exception handlers
- Throw `ExpectedException` for business errors
- Configure SDK behavior in `application.yml`
- Understand how automatic HTTP request/response logging works

## SDK Features

| Feature | What it does | Toggle |
|---------|-------------|--------|
| **Logging** | Attaches a UUID `Log-ID` header to every HTTP request/response and logs them automatically | `sdk.logging.enabled` |
| **Response Wrapper** | Wraps controller return values in `CommonApiResponse` automatically | `sdk.response.enabled` |
| **Exception Handler** | Converts `ExpectedException` into a standard error response | `sdk.exception.enabled` |
| **Swagger** | Auto-configures `/v3/api-docs` and `/swagger-ui` | `sdk.swagger.enabled` |

## Quick Reference

- [`references/common-api-response.md`](references/common-api-response.md) — `CommonApiResponse` factory methods and controller usage patterns
- [`references/expected-exception.md`](references/expected-exception.md) — `ExpectedException` throw patterns and HTTP status mapping
- [`references/sdk-config.md`](references/sdk-config.md) — `application.yml` SDK configuration with all available options
