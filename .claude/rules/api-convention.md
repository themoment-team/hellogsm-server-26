# API Convention Rules — hellogsm-server-25

> Scope: `server` module only (the HTTP API surface). `entrance-*` modules expose no HTTP API of their own — `entrance-batch` runs as a DB-backed batch job, invoked from `server`.

## URL Structure
```
/{domain}/v3/{resource}[/{id}][/{sub-resource}]
```
- **Versioning:** always `/v3` suffix
- **Lowercase kebab-case** path segments
- **Plural nouns** for collection resources, **singular** for actions

## Endpoint Patterns

| HTTP Method | Path pattern                              | Purpose                        |
|-------------|-------------------------------------------|--------------------------------|
| `GET`       | `/oneseo/v3/oneseo/{memberId}`            | Get single resource            |
| `GET`       | `/oneseo/v3/oneseo/me`                    | Get current user's resource    |
| `GET`       | `/oneseo/v3/oneseo/search?page=0&size=10` | Paginated list                 |
| `POST`      | `/oneseo/v3/oneseo/me`                    | Create for current user        |
| `PUT`       | `/oneseo/v3/oneseo/{memberId}`            | Full replacement update        |
| `PATCH`     | `/oneseo/v3/arrived-status/{memberId}`    | Partial / status update        |
| `DELETE`    | `/member/v3/member/{memberId}`            | Delete resource                |
| `POST`      | `/operation/v3/operation/announce-first-test-result` | Execute action    |

## Request Binding

```java
// Path variable
@PathVariable Long memberId

// Query parameter
@RequestParam(defaultValue = "0") int page

// Request body — always @Valid
@RequestBody @Valid CreateMemberReqDto reqDto

// Authenticated user ID (custom annotation)
@AuthRequest Long memberId
```

## Response Wrapper — `CommonApiResponse`

Controllers follow a split pattern — never use `ResponseEntity`:

| Operation type | Return type | Example |
|----------------|-------------|---------|
| Write (create / update / delete) | `CommonApiResponse` | `return CommonApiResponse.created("생성되었습니다.");` |
| Read (query / get) | DTO directly | `return service.execute(memberId);` |

```java
// Write — CommonApiResponse (message only, no data)
public CommonApiResponse create(@RequestBody @Valid OneseoReqDto reqDto) {
    service.execute(reqDto);
    return CommonApiResponse.created("생성되었습니다.");
}

// Read — DTO returned directly
public FoundMemberResDto find(@AuthRequest Long memberId) {
    return service.execute(memberId);
}
```

**`CommonApiResponse` method signatures:**
```java
CommonApiResponse.success(String message)   // HTTP 200, data = null
CommonApiResponse.created(String message)   // HTTP 201, data = null
CommonApiResponse.error(String message, HttpStatus status)
```

There is **no** `success(message, data)` overload. Read endpoints return DTOs directly, not wrapped.

### CommonApiResponse JSON shape
```json
{
  "status": "OK",
  "code": 200,
  "message": "수정되었습니다.",
  "data": null
}
```
- `data` is always `null` for `success()` and `created()` — omitted via `@JsonInclude(NON_NULL)`

## Swagger / OpenAPI Annotations

```java
@Tag(name = "Member", description = "회원 관련 API")
@RestController
public class MemberController {

    @Operation(summary = "회원 조회", description = "ID로 회원 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "회원 없음")
    })
    @GetMapping("/{memberId}")
    public FoundMemberResDto findMember(
            @Parameter(description = "회원 ID") @PathVariable Long memberId) { ... }
}
```

- `@Tag` description: Korean
- `@Operation` summary/description: Korean
- All Swagger annotations are **required** on new endpoints

## Pagination
- Use Spring Data `Pageable` for list endpoints
- Default: `page=0`, `size=10`
- Response wraps `Page<T>` or `List<T>` inside `CommonApiResponse`

## Date / Time
- JSON format: `yyyy-MM-dd` for `LocalDate`
- Annotate DTO fields: `@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")`
- Timezone: `Asia/Seoul`

## Prohibited Patterns
- No bare `200 OK` without `CommonApiResponse` wrapper
- No direct entity exposure in responses — always use DTO
- No non-versioned endpoints (must include `/v3`)
- No `@ResponseBody` in controllers — use `@RestController`