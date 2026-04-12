# CommonApiResponse — Reference

Import: `team.themoment.sdk.response.CommonApiResponse`

## Factory Methods

```java
CommonApiResponse.success(String message)              // HTTP 200
CommonApiResponse.created(String message)              // HTTP 201
CommonApiResponse.error(String message, HttpStatus status)
```

> There is **no** `success(message, data)` overload. Read endpoints return DTOs directly.

## JSON Shape

```json
{
  "status": "OK",
  "code": 200,
  "message": "수정되었습니다.",
  "data": null
}
```

`data` is always `null` for `success()` and `created()` — omitted in JSON via `@JsonInclude(NON_NULL)`.

## Controller Usage

Controllers follow a split pattern — **never use `ResponseEntity`**:

```java
// Write operation — return CommonApiResponse
@PostMapping("/me")
public CommonApiResponse createOneseo(@RequestBody @Valid OneseoReqDto reqDto) {
    createOneseoService.execute(reqDto);
    return CommonApiResponse.created("생성되었습니다.");
}

// Update operation
@PutMapping("/{memberId}")
public CommonApiResponse updateOneseo(
        @PathVariable Long memberId,
        @RequestBody @Valid OneseoReqDto reqDto) {
    updateOneseoService.execute(memberId, reqDto);
    return CommonApiResponse.success("수정되었습니다.");
}

// Read operation — return DTO directly (SDK wraps it automatically)
@GetMapping("/{memberId}")
public FoundOneseoResDto findOneseo(@PathVariable Long memberId) {
    return findOneseoService.execute(memberId);
}
```

## Exception Handler Usage

```java
@ExceptionHandler(RuntimeException.class)
public CommonApiResponse unExpectedException(RuntimeException ex) {
    log.error("UnExpectedException Occur : ", ex);
    return CommonApiResponse.error("internal server error has occurred", HttpStatus.INTERNAL_SERVER_ERROR);
}
```