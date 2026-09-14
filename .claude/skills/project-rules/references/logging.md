# Logging Rules — hellogsm-server-25

> Scope: Java/Spring modules (`server`, `persistence`) only. `entrance-engine`/`entrance-dsl`/`entrance-plans` are pure functions with no logging; `entrance-batch` may log its run but follows its own conventions in [`entrance.md`](./entrance.md).

## Library
- SLF4J via Lombok `@Slf4j` annotation
- Logger field name: `log` (Lombok default)
- Manual `LoggerFactory.getLogger()` only in filters/non-Spring beans

## Log Levels

| Level  | When to use                                                         |
|--------|---------------------------------------------------------------------|
| `INFO` | Root default; request/response in `LoggingFilter`                  |
| `WARN` | Expected business errors, duplicate data detected, handled edge cases |
| `ERROR` | Unexpected runtime exceptions, system failures, async failures     |
| `TRACE` | Detailed exception stack traces (dev only, never in prod logs)     |
| `DEBUG` | Not currently used — avoid adding without configuration change     |

## Usage Patterns

### Service layer — expected edge case
```java
@Slf4j
@Service
public class CreateMemberService {
    public void execute(...) {
        if (duplicate) {
            log.warn("중복 회원 삭제: phoneNumber={}", phoneNumber);
        }
    }
}
```

### Exception handler — expected vs unexpected
```java
// GlobalExceptionHandler
log.warn("Expected exception: {}", e.getMessage());
log.error("Unexpected exception", e);
log.trace("Exception detail: ", e);   // stack trace only at trace
```

### Async failures
```java
// GlobalAsyncExceptionHandler
log.error("Async method [{}] threw exception: {}", methodName, e.getMessage());
```

## Format Rules
- Always use parameterized logging: `log.info("id={}", id)` — never string concatenation
- Include relevant identifiers (memberId, oneseoId) in warn/error messages
- Korean messages are acceptable for business-logic warn/error (matches existing codebase)
- Do not log sensitive data: passwords, tokens, full resident numbers

## Configuration
- Config file: `src/main/resources/logback-spring.xml`
- Profile `default` → Console appender
- Profile `dev` → AWS CloudWatch (`hellogsm-stage-log`)
- Profile `prod` → AWS CloudWatch (`hellogsm-prod-log`)
- Batch size: 50, flush: 10s, retries: 5

## Prohibited Patterns
- `System.out.println(...)` — forbidden
- `e.printStackTrace()` — forbidden; use `log.error("...", e)` or `log.trace("...", e)`
- String concatenation in log args: `log.info("id=" + id)` — use parameterized form
- Logging inside tight loops without guard (`if (log.isDebugEnabled())`)