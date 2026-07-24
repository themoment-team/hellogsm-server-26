# Coding Style Rules — hellogsm-server-25

> Scope: Java/Spring modules (`server`, `persistence`) only. The Kotlin `entrance-*` modules follow [`entrance/CLAUDE.md`](../../entrance/CLAUDE.md) instead (DSL-is-data principle, `BigDecimal`-only scoring, plan-file-per-year policy).

## Language & Framework
- Java 25, Spring Boot 4.0.5, Gradle
- Jakarta EE (not `javax`) for validation, persistence annotations
- Lombok is mandatory — never write boilerplate manually

## Class-Level Annotations

### Controller
```java
@Tag(name = "...", description = "...")   // Korean descriptions for Swagger
@RestController
@RequestMapping("/endpoint/v3")
@RequiredArgsConstructor
public class XxxController { }
```

### Service
```java
@Service
@RequiredArgsConstructor
// @Slf4j only when the class actually logs
public class ActionFeatureService { }
```

### Entity
```java
@Getter
@Builder
@Entity
@Table(name = "tb_entity_name")           // tb_ prefix, snake_case
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class EntityName { }
```

### Request DTO (record)
```java
public record ActionEntityReqDto(
    @NotBlank String fieldName,
    @NotNull EnumType enumField
) { }
```

### Response DTO (record)
```java
public record FoundEntityResDto(
    Long id,
    String name
) { }
```

## Naming Conventions

| Component       | Pattern                          | Example                              |
|-----------------|----------------------------------|--------------------------------------|
| Controller      | `{Feature}Controller`            | `MemberController`                   |
| Service         | `{Action}{Feature}Service`       | `CreateMemberService`                |
| Request DTO     | `{Entity}{Action}ReqDto`         | `CreateMemberReqDto`                 |
| Response DTO    | `{ResultDesc}ResDto`             | `FoundMemberResDto`                  |
| Internal DTO    | `{Description}Dto`               | `MiddleSchoolAchievementCalcDto`      |
| Repository      | `{Entity}Repository`             | `MemberRepository`                   |
| Entity          | `{DomainName}`                   | `Member`, `Oneseo`                   |
| Enum Type       | `{Domain}Type` or plain noun     | `SexType`, `MajorType`               |
| DB table        | `tb_{snake_case}`                | `tb_member`, `tb_oneseo`             |

## Lombok Rules
- Use `@RequiredArgsConstructor` instead of manual constructors
- Use `@Getter` on entities (no `@Setter` on entities)
- Use `@Builder` on entities for test object construction
- Use `@Slf4j` only on classes that actually emit log statements
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` on JPA entities

## Immutability
- Prefer Java records for DTOs (request and response)
- Never expose `@Setter` on JPA entities; use dedicated update methods instead
- Enum fields on entities must use `@Enumerated(EnumType.STRING)`

## Code Formatting (Spotless)
- 4-space indentation (no tabs)
- Import order (Spotless sort groups): `java` → `javax` → `org` → `com` → blank — `jakarta.*` falls in the blank group
- Remove unused imports automatically (`./gradlew spotlessApply`)
- Trim trailing whitespace, end files with newline
- Run `./gradlew spotlessApply` before committing

## Spring Data JPA
- Use `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` / `@LastModifiedDate`
- QueryDSL for complex queries — keep in `custom/impl/` package
- Repository interface extends `JpaRepository` and optionally `Custom{Entity}Repository`

## Validation
- Use Jakarta Bean Validation on records: `@NotBlank`, `@NotNull`, `@Pattern`, `@NotDate`
- Custom validators live in `domain/{feature}/annotation/`
- Always annotate controller parameters with `@Valid` when binding DTOs

## Prohibited Patterns
- No `System.out.println` — use `log.*` via `@Slf4j`
- No raw `new RuntimeException` — use `ExpectedException`
- No `@Setter` on JPA entities
- No `javax.*` imports — use `jakarta.*`