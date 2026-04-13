# ExpectedException — Reference

Import: `team.themoment.sdk.exception.ExpectedException`

## Constructor Signatures

```java
// With descriptive message (preferred)
new ExpectedException(String message, HttpStatus statusCode)

// Status only (when status is self-explanatory)
new ExpectedException(HttpStatus statusCode)
```

Stack trace is skipped for performance (`fillInStackTrace()` returns `this`).

## Throw Patterns

```java
// Resource not found — include the offending ID
throw new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND);

// Conflict — duplicate resource
throw new ExpectedException("이미 원서를 제출하였습니다.", HttpStatus.CONFLICT);

// Bad request — business rule failure
throw new ExpectedException("엑셀 파일이 비어있습니다.", HttpStatus.BAD_REQUEST);

// Unauthorized — unauthenticated access
throw new ExpectedException(HttpStatus.UNAUTHORIZED);

// Forbidden — insufficient permissions
throw new ExpectedException(HttpStatus.FORBIDDEN);
```

## HTTP Status Mapping

| Scenario | HttpStatus |
|----------|-----------|
| Resource not found | `NOT_FOUND` (404) |
| Invalid input / business rule failure | `BAD_REQUEST` (400) |
| Unauthenticated access | `UNAUTHORIZED` (401) |
| Insufficient permissions | `FORBIDDEN` (403) |
| Duplicate resource | `CONFLICT` (409) |
| Internal server fault | `INTERNAL_SERVER_ERROR` (500) |

## Error Response Shape

The SDK exception handler converts `ExpectedException` into:

```json
{
  "status": "NOT_FOUND",
  "code": 404,
  "message": "존재하지 않는 지원자입니다. member ID: 42",
  "data": null
}
```

## Rules

- Messages should be Korean for business errors
- Include the offending identifier when relevant: `"... member ID: " + memberId`
- Never create subclasses — use `ExpectedException` for all business errors
- Never throw `RuntimeException` directly