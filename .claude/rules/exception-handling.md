# Exception Handling Rules — hellogsm-server-25

> Scope: Java/Spring modules (`server`, `persistence`) only. `entrance-*` (Kotlin) has no equivalent custom exception type — see [`entrance/CLAUDE.md`](../../entrance/CLAUDE.md) (`PlanValidationException` for plan validation errors).

## Exception Hierarchy
There is exactly **one custom exception class** in this project:

```java
@Getter
public class ExpectedException extends RuntimeException {
    private final HttpStatus statusCode;

    public ExpectedException(String message, HttpStatus statusCode) { ... }
    public ExpectedException(HttpStatus statusCode) { ... }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;  // Skip stack trace for performance
    }
}
```

**Never create additional custom exception subclasses.** All business errors go through `ExpectedException`.

## Throwing Exceptions

### With a descriptive message (preferred)
```java
throw new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND);
```

### Message-only status (when status is self-explanatory)
```java
throw new ExpectedException(HttpStatus.UNAUTHORIZED);
```

## Message Format
- Korean messages for business errors
- Include the offending identifier when relevant: `"존재하지 않는 지원자입니다. member ID: " + memberId`
- Keep messages concise and user-readable

## HTTP Status Usage

| Scenario                              | HttpStatus                    |
|---------------------------------------|-------------------------------|
| Resource not found                    | `NOT_FOUND` (404)             |
| Invalid input / business rule failure | `BAD_REQUEST` (400)           |
| Unauthenticated access                | `UNAUTHORIZED` (401)          |
| Insufficient permissions              | `FORBIDDEN` (403)             |
| Conflict (duplicate resource)         | `CONFLICT` (409)              |
| Internal server fault                 | `INTERNAL_SERVER_ERROR` (500) |

## Error Response Format (`CommonApiResponse`)
```json
{
  "status": "NOT_FOUND",
  "code": 404,
  "message": "존재하지 않는 지원자입니다. member ID: 42",
  "data": null
}
```

## Global Exception Handler (`GlobalExceptionHandler`)
- `@RestControllerAdvice` centralizes all handling — do NOT add local try/catch in controllers
- Handled exception types:

| Exception                          | Response status              | Log level |
|------------------------------------|------------------------------|-----------|
| `ExpectedException`                | exception's `statusCode`     | WARN      |
| `MethodArgumentNotValidException`  | BAD_REQUEST (400)            | WARN      |
| `NoHandlerFoundException`          | NOT_FOUND (404)              | WARN      |
| `MaxUploadSizeExceededException`   | BAD_REQUEST (400)            | WARN      |
| `IllegalStateException`            | UNAUTHORIZED (401)           | WARN      |
| `RuntimeException` (catch-all)     | INTERNAL_SERVER_ERROR (500)  | ERROR     |

## Async Exception Handler
- `GlobalAsyncExceptionHandler` handles `@Async` method failures
- Logs at `error` level with method name
- Does NOT return a response (async fire-and-forget)

## Prohibited Patterns
- Do not create new exception subclasses
- Do not swallow exceptions with empty catch blocks
- Do not use checked exceptions for business logic
- Do not throw `RuntimeException` directly — use `ExpectedException`
- Do not add try/catch in controllers — delegate to `GlobalExceptionHandler`