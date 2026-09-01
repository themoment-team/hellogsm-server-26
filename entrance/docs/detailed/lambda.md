# entrance-lambda — 모의 성적 계산 API (사용법)

`entrance-lambda`는 지원자가 원서 작성 중 "내 성적으로 몇 점 나오는지" 미리 계산해보는 **모의
성적 계산 API**다. 기존 `go-hellogsm-score-calculator`(Go, AWS Lambda)를 대체한다.

- **Spring을 쓰지 않는다.** `aws-lambda-java-core`의 `RequestHandler`를 직접 구현했다 —
  server가 다운되어도 이 모의 계산 API는 동작해야 한다는 기존 가용성 요건 때문에, 콜드 스타트를
  최소화하려고 프레임워크 부트스트랩 비용을 지지 않는다.
- **엔진은 배치와 동일하다.** `ScoringEngine(plan)` 하나뿐이다 — 성적 계산 로직이 배치와
  갈릴 일이 없다.
- **API Gateway REST API(v1 프록시 통합) 계약을 전제로 한다.** Lambda Function URL이나
  HTTP API(v2 payload)로 앞단을 구성하면 이벤트 스키마가 달라 그대로 동작하지 않는다.

## 요청 / 응답 계약

기존 Go 구현과 **JSON 필드명·인증 헤더가 완전히 동일**하다 — 이 계약이 깨지면 server 코드
변경 없이는 전환할 수 없으므로 임의로 바꾸지 않는다.

- 인증: 헤더 `x-hg-api-key`가 환경변수 `X_HG_INTERNAL_API_KEY`와 일치해야 한다 (`401` 아니면 통과).
- 요청 바디(`ScoreCalculatorRequest`): `achievement1_1~3_2`(학기별 성취도 리스트), `artsPhysicalAchievement`,
  `absentDays`/`attendanceDays`, `volunteerTime`, `gedAvgScore`, `graduationType`(`CANDIDATE`/`GRADUATE`/`GED`).
  - `achievement1_1`은 plan이 직접 채점하지 않는 학기(1-2부터 반영)지만, **결측 학기 대체**
    (`missingSemester(SAME_YEAR_OTHER_SEMESTER, ...)`)의 원본으로 쓰일 수 있어 그대로 받아
    엔진에 전달한다.
- 응답 바디(`ScoreCalculatorResponse`): `generalSubjectsScore`, `generalSubjectsScoreDetail`
  (학기별 1-2~3-2 상세, plan에 없는 학기는 `0.000`), `artsPhysicalSubjectsScore`, `totalSubjectsScore`,
  `attendanceScore`, `volunteerScore`, `totalScore`. 검정고시는 교과 관련 필드가 전부 `null`로
  생략된다(기존 Go 응답과 동일).
- 오류 응답(`ErrorResponse`): `error`, `message`, `code`(`UNAUTHORIZED`/`EMPTY_BODY`/`INVALID_JSON`/`VALIDATION_ERROR`).

## 로컬에서 AWS 없이 호출해보기

`ScoreCalculatorHandler`는 API Gateway 이벤트 객체 하나만 로컬에서 만들어주면 되므로, 실제
Lambda에 배포하지 않고도 요청/응답을 그대로 확인할 수 있다.

```kotlin
val handler = ScoreCalculatorHandler(expectedApiKey = "test-api-key")
val event = APIGatewayProxyRequestEvent()
    .withBody(objectMapper.writeValueAsString(ScoreCalculatorRequest(achievement1_2 = listOf(3, 4, 5, ...), ...)))
    .withHeaders(mapOf("x-hg-api-key" to "test-api-key"))

val response = handler.handleRequest(event, FakeContext())
println(response.statusCode) // 200
println(response.body)       // ScoreCalculatorResponse의 JSON
```

`ScoreCalculatorHandlerTest`에 위와 같은 케이스가 이미 있으니 그대로 참고하면 된다.

```bash
./gradlew :entrance-lambda:test --tests "*ScoreCalculatorHandlerTest*"
```

## 빌드 / 배포

```bash
./gradlew :entrance-lambda:shadowJar
# → entrance/entrance-lambda/build/libs/entrance-lambda-*.jar (전 의존성 포함 fat jar)
```

- CI/CD: `.github/workflows/entrance-lambda-{stage,prod}-{ci,cd}.yml`가 빌드·배포 워크플로를
  담당한다. 다만 **AWS 함수 생성·IAM 역할·API Gateway 연결은 콘솔/IaC로 최초 1회 준비해야 하는
  인프라 작업**이라 CI/CD 밖이다 — 워크플로는 이미 존재하는 함수의 코드 갱신만 한다.
- 전환 방식: 기존 Go 함수와 **병행 배포**한다. 요청/응답 계약이 동일하므로, 검증 후 전환은
  server의 `SCORE_CALCULATOR_SERVICE_URL` 환경변수를 새 Lambda의 API Gateway 엔드포인트로
  바꾸는 것만으로 충분하다 — **server 코드 변경 불필요**.

## 남은 것

- AWS 실배포(함수·IAM·API Gateway 연결) — 코드 밖 인프라 작업.
- 콜드 스타트 실측 — SnapStart 우선 적용, 미달 시 GraalVM native 검토.

더 넓은 맥락(왜 별도 배포 아티팩트로 유지하는지, entrance-batch와의 관계)은
[architecture.md](architecture.md) 참고.
