# 코드 개선점 점검 보고서

> 작성일: 2026-06-16
> 범위: `src/main/java` 서비스/보안/필터 계층 중심 정적 점검
> 각 항목은 **실제 문제인지 / 비즈니스 로직상 의도된 동작인지** 코드 근거로 판정함

판정 범례
- ✅ **실제 문제** — 수정 권장
- ⚠️ **잠재 문제** — 현재는 다른 조건에 의해 가려져 있으나 논리적으로 잘못됨
- 🟡 **개선 여지** — 동작은 정상이나 견고성/성능 측면 개선 가능
- ☑️ **의도된 동작** — 문제 아님 (오해 방지용 기록)

---

## 1. ⚠️ `validateSecondTestResultAnnouncement()`가 1차 시험 기준값을 참조 (복붙 추정 버그)

**위치**: `domain/oneseo/service/OneseoService.java:168-174`

```java
public boolean validateSecondTestResultAnnouncement() {
    return operationTestResultRepository.findTestResult()
            .map(testResult -> testResult.getSecondTestResultAnnouncementYn().equals(NO)
                    || LocalDateTime.now().isBefore(scheduleEnv.firstResultsAnnouncement())   // ← 1차 발표시각
                    || entranceTestResultRepository.existsByFirstTestPassYnIsNull())           // ← 1차 합격여부
            .orElse(true);
}
```

**판정 근거**
- 이 메서드는 `QueryMySecondTestResultService`에서 **2차 결과를 지원자에게 노출할지** 결정하는 게이트다 (`true`면 `null` 반환 = 미공개).
- 그런데 두 번째·세 번째 조건이 2차가 아닌 **1차** 기준을 본다:
  - `scheduleEnv.firstResultsAnnouncement()` → 2차라면 `finalResultsAnnouncement()`가 맞다 (`ScheduleEnvironment` 레코드에 두 필드 모두 존재: `ScheduleEnvironment.java:8-10`).
  - `existsByFirstTestPassYnIsNull()` → 2차라면 `existsByFirstTestPassYnAndSecondTestPassYnIsNull(YES)` 류가 맞다.
- 같은 도메인의 **정식 2차 발표 검증** 로직(`AnnounceSecondTestResultService.validateSecondTestResultAnnouncementPeriod()`, `AnnounceSecondTestResultService.java:40-47`)은 올바르게 `finalResultsAnnouncement()` + 2차 합격여부 + 1차 발표완료를 모두 확인한다. 즉 동일 의미의 검증이 이미 한쪽에 존재하므로, `OneseoService` 쪽은 **복사 과정의 누락**으로 보인다.

**실제 영향**: 첫 번째 조건 `secondTestResultAnnouncementYn == NO`가 1차 게이트 역할을 하고, 이 플래그는 엄격한 검증을 통과한 `AnnounceSecondTestResultService`로만 `YES`가 되므로 정상 시나리오에서는 결과가 의도대로 가려진다. 따라서 현재 운영상 즉시 장애로 드러나지는 않으나, **시각 게이트가 1차 발표시각으로 잘못 걸려 있어** 일정 구성에 따라 노출 시점이 어긋날 수 있는 잠재 버그다.

**권장**: `firstResultsAnnouncement()` → `finalResultsAnnouncement()`, 1차 합격여부 체크 → 2차 미결정자 체크로 교정. `AnnounceSecondTestResultService`의 검증 로직과 의미를 일치시킬 것.

---

## 2. ✅ SMS 인증 코드 생성에 `java.util.Random` 사용 (보안)

**위치**
- `domain/member/service/GenerateCodeService.java:42-44` (`getRandomCode`)
- `domain/member/service/impl/GenerateCodeServiceImpl.java:24` — `private static final Random RANDOM = new Random();`
- `domain/member/service/impl/GenerateTestCodeServiceImpl.java:20` — 동일

```java
protected String getRandomCode(Random RANDOM) {
    return String.format("%0" + DIGIT_NUMBER + "d", RANDOM.nextInt(0, MAX + 1));
}
```

**판정 근거**
- 생성되는 값은 **본인확인용 6자리 인증번호**(SMS 발송)다. 보안 토큰에 해당한다.
- `java.util.Random`은 48비트 선형합동(LCG) PRNG로 **예측 가능**하다. 몇 개의 출력만 관측하면 내부 시드 복원이 가능해, 인증번호 추정 공격에 노출될 수 있다.
- 6자리(0~999999)라 엔트로피 자체가 크지 않으므로(무차별 대입 여지) PRNG 예측 가능성까지 더해지면 위험이 가중된다.

**의도 여부**: 코드 어디에도 "예측 가능성을 허용한다"는 의도는 없으며, 보안 기본 원칙상 인증 코드는 `SecureRandom`을 써야 한다. → 의도된 설계가 아니라 개선 대상.

**권장**
- `java.util.Random` → `java.security.SecureRandom`으로 교체.
- 더불어 6자리 인증번호에 대한 **시도 횟수 제한/만료**가 충분한지 함께 점검(아래 4번과 연관).

---

## 3. 🟡 외부 Feign 호출을 DB 트랜잭션(+비관적 락) 내부에서 동기 수행

**위치**
- `domain/oneseo/service/CreateOneseoService.java:45-51, 147` — `@Transactional` + `findByIdForUpdateOrThrow`(비관적 쓰기 락) 안에서 `lambdaScoreCalculatorClient.calculateScore(...)` 호출
- `domain/oneseo/service/ModifyOneseoService.java:41, 138` — `@Transactional` 안에서 동일 호출

**판정 근거**
- `calculateScore`는 외부 AWS Lambda를 호출하는 동기 HTTP(Feign) 요청이다(`LambdaScoreCalculatorClient`).
- 이 호출이 트랜잭션 경계 안에서 일어나므로, 외부 서비스가 느리거나 응답 지연되면 **DB 커넥션과 트랜잭션이 그만큼 오래 점유**된다.
- 특히 `CreateOneseoService`는 `findByIdForUpdateOrThrow`로 **member 행에 비관적 락**을 잡은 상태라, 외부 호출 지연 동안 해당 락이 유지되어 동시성/처리량에 직접 영향을 준다.
- 원서 접수 마감 직전처럼 트래픽이 몰리는 구간에서 커넥션 풀 고갈/락 대기 폭증으로 이어질 수 있다.

**의도 여부**: 점수 계산 결과를 같은 트랜잭션에서 저장해야 하는 정합성 요구가 있어 **현재 구조 자체는 합리적**이다(즉 명백한 버그는 아님). 다만 외부 호출을 트랜잭션 밖으로 빼는 패턴(먼저 계산 → 결과를 받아 짧은 트랜잭션에서 저장)이 견고성 측면에서 낫다.

**권장**
- 외부 점수 계산 호출을 트랜잭션 진입 전에 수행하고, 그 결과만 짧은 쓰기 트랜잭션에서 저장하도록 분리 검토.
- 분리가 어렵다면 Feign 타임아웃을 짧게 설정하고(타임아웃/재시도 정책 점검), 트랜잭션 타임아웃을 명시.

---

## 4. 🟡 인증 코드 발급의 비원자성(rate-limit 체크-갱신 경합 + 트랜잭션 부재)

**위치**: `domain/member/service/impl/GenerateCodeServiceImpl.java:26-47`

```java
public String execute(Long memberId, GenerateCodeReqDto reqDto) {   // @Transactional 없음
    AuthenticationCode authenticationCode = codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP)...
    if (isLimitedRequest(authenticationCode)) throw ...;   // 조회 후
    ...
    codeRepository.save(...);                               // 갱신 (check-then-act 비원자)
    sendCodeNotificationService.execute(phoneNumber, code); // 외부 SMS 발송
    ...
}
```

**판정 근거**
- 메서드에 `@Transactional`이 없고, `count >= LIMIT_COUNT_CODE_REQUEST` 체크와 저장이 분리되어 있어 **동시 요청 시 횟수 제한을 우회**할 수 있다(같은 member로 동시에 여러 발급 요청).
- 저장 직후 SMS 발송이 일어나므로, 발송 단계 실패와 저장 사이의 정합성 보장이 없다(부분 성공 가능).

**의도 여부**: 명시적 의도로 보긴 어렵고, 동시성이 낮은 도메인 특성상 그동안 문제가 드러나지 않았을 가능성이 높다. → 견고성 개선 대상.

**권장**: 발급 카운트 증가를 원자적으로 처리(DB 락 또는 원자적 UPDATE/Redis 카운터)하고, 트랜잭션 경계를 명확히. SMS 발송 실패 시의 처리 정책 정의.

---

## 5. 🟡 `@CachePut`과 `@Transactional`의 캐시-DB 정합성

**위치**: `CreateOneseoService.java:45-47`, `ModifyOneseoService.java:41-42` (그 외 `@Cacheable`/`@CacheEvict` 다수)

**판정 근거**
- `@CachePut`은 메서드 정상 반환 시 반환값을 캐시에 기록한다. 스프링 캐시 추상화는 기본적으로 **트랜잭션 커밋과 동기화되지 않는다**.
- 따라서 메서드 반환 이후 커밋이 실패(예: 제약 위반, 커밋 시점 오류)하면 **DB에는 반영되지 않은 값이 캐시에는 남아** 일시적 불일치가 생길 수 있다.

**의도 여부**: 일반적으로 인지하기 어려운 엣지 케이스이며 현재 명백한 장애 근거는 없음 → 인지·점검 항목으로 기록.

**권장**: 캐시 쓰기를 트랜잭션 커밋 이후로 동기화(`TransactionAwareCacheManagerProxy` 등)할지 검토. 우선순위는 낮음.

---

## 6. 🟡 `saveCalculatedScoreToDb` 갱신 분기의 null 가드 비대칭(NPE 가능성)

**위치**: `domain/oneseo/service/CreateOneseoService.java:154-238`

**판정 근거**
- **신규 저장 분기**(157-195)는 `attendanceScore`/`volunteerScore`가 null일 수 있음을 전제로 `!= null` 가드 후 `add` 한다(169-172).
- 그러나 **갱신 분기**(196-238)의 `calculatedScore.attendanceScore().add(calculatedScore.volunteerScore())`(210, 230)는 **가드 없이** 호출한다. 두 값 중 하나라도 null이면 NPE.
- 같은 값에 대해 한쪽은 null 가드를 두고 다른 쪽은 두지 않은 **비대칭**이 핵심 근거다.

**의도 여부 확인 필요**: 외부 Lambda 응답에서 GED/비-GED 케이스에 `attendanceScore`/`volunteerScore`가 항상 non-null로 보장된다면 실질 위험은 없다(의도된 전제). 하지만 그 보장이 코드/계약상 명시돼 있지 않고, 신규 분기 스스로가 null 가능성을 인정하고 있어 **방어적으로 통일**하는 편이 안전하다.

**권장**: 갱신 분기에도 동일한 null 가드를 적용하거나, Lambda 응답 계약에서 두 값이 non-null임을 명시/검증.

---

## 7. ☑️ (오해 방지) `CreateMemberService`의 중복 전화번호 회원 삭제

**위치**: `domain/member/service/CreateMemberService.java:42-63`

- `member.getRole() == APPLICANT`면 "이미 회원가입" 예외, 이후 `ifDuplicateMemberDeleteMemberInfo`로 **동일 전화번호의 다른 회원** 정보를 삭제하는 흐름은 의도된 비즈니스 로직이다(전화번호 재사용 시 기존 미완료 가입 정리). 또한 `oneseoSubmissionEnd` 이후 또는 1차 합격여부 산출 이후에는 삭제를 막는 가드(54-56)가 있어 안전장치도 갖춰져 있다. → **문제 아님**.

---

## 요약 (우선순위)

| # | 항목 | 판정 | 우선순위 |
|---|------|------|----------|
| 2 | 인증 코드에 `java.util.Random` 사용 | ✅ 실제 문제(보안) | 높음 |
| 1 | `validateSecondTestResultAnnouncement` 1차 기준 참조 | ⚠️ 잠재 버그 | 높음 |
| 3 | 트랜잭션/락 내부 외부 Feign 동기 호출 | 🟡 개선 | 중간 |
| 4 | 인증 코드 발급 비원자성(rate-limit 경합) | 🟡 개선 | 중간 |
| 6 | 점수 저장 갱신 분기 null 가드 비대칭 | 🟡 개선(확인 필요) | 중간 |
| 5 | `@CachePut`-트랜잭션 정합성 | 🟡 개선 | 낮음 |

> 본 보고서는 정적 분석 기반이며, 1·6번은 운영 데이터/외부 계약 확인으로 영향 범위를 최종 검증하는 것을 권장함.