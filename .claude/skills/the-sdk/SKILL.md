---
description: the-sdk 공통 라이브러리 사용 가이드
---

# the-sdk 사용 가이드

`com.github.themoment-team:the-sdk:1.5` — 이 프로젝트의 핵심 공통 라이브러리입니다.  
`application.yml`의 `sdk.*` 설정으로 제어합니다.

## 기능

- **Logging**: 모든 HTTP 요청/응답에 UUID `Log-ID` 부여, 자동 로깅
- **Response Wrapper**: 컨트롤러 반환값을 `CommonApiResponse`로 자동 래핑. `success()` / `created()` / `error()` 팩토리 메서드 사용
- **Exception Handler**: `ExpectedException(message, HttpStatus)`를 throw하면 표준 에러 응답 자동 반환
- **Swagger**: `/v3/api-docs`, `/swagger-ui` 경로에 자동 구성. `/v1/**` 경로만 문서화
