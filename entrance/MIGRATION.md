# MIGRATION.md — `every-entrance` → `hellogsm-server-26` 저장소 통합 계획

이 문서는 `every-entrance`를 `hellogsm-server-26`에 Gradle 멀티모듈로 흡수하는 절차를 다룬다.
결정 배경은 [CONTEXT.md](./CONTEXT.md) 결정 사항, 설계 원칙은 [CLAUDE.md](./CLAUDE.md) 참고.

---

## 1. 결정 요약

| 항목 | 결정 |
|---|---|
| 통합 여부 | **통합한다** (독립 라이브러리 + JitPack 배포 기각) |
| 방향 | `every-entrance` **→** `hellogsm-server-26` (작은 쪽이 큰 쪽으로 이동) |
| 수단 | `git subtree` — 양쪽 히스토리 모두 보존 |
| 살아남는 레포 | `themoment-team/hellogsm-server-26` (remote·CI/CD·CodeDeploy 그대로) |

### 기각된 대안

**A. 독립 라이브러리 유지 + JitPack 배포**
소비자가 서버 하나뿐이라 릴리스 사이클이 값을 못 산다. 더 큰 문제는 스키마 매핑이 3중이 되는 것 — `go-hellogsm`이 `tb_oneseo` 등을 자체 매핑하고, 서버가 JPA 엔티티(`Oneseo`, `EntranceTestResult`, `MiddleSchoolAchievement`)로 매핑하는데, `entrance-batch`가 별도 레포에 있으면 같은 공유 MySQL에 세 번째 매핑이 생긴다. 원서접수(10월)·평가(11월) 성수기에 엔진 버그를 고칠 때 태그 릴리스를 기다려야 하는 것도 순수 손해다.

**B. 새 저장소를 만들어 서버를 모듈로 편입**
"새 저장소라 갈아엎을 게 없다"는 기대와 반대로, 갈아엎기가 **최대화**된다. 서버를 모듈로 만드는 작업(`src/` → `server/`, 빌드 재구성)은 어느 레포에서 하든 동일하고, 새 레포는 거기에 다음을 추가로 얹는다.

- GitHub Actions 워크플로 4개(prod/stage × CI/CD) 재구성
- 시크릿 7개 재등록 — `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` / `AWS_REGION` / `BUCKET_NAME` / `PROD_WEB_YML` / `DEV_WEB_YML` / `DISCORD_WEBHOOK`
- AWS CodeDeploy 애플리케이션·배포그룹 재연결
- 브랜치 보호 규칙, PR·이슈 템플릿, `.claude/rules/*`·`.agents/skills/` 팀 툴링 이전
- 진행 중 브랜치 무효화(`develop`, `feature/chore/remove-the-sdk`, `feature/fix/local-cors-cookie`)와 기여자 18명 전원의 re-clone

규모 비대칭이 방향을 결정한다 — every-entrance는 8커밋·1인·remote 없음·미배포, 서버는 1,882커밋·18인·프로덕션 운영 중이다. 입시 도메인은 "이 규칙이 왜 이렇게 쓰였나"를 되짚어야 하는 코드라 2년치 `git blame`이 끊기는 비용도 크다.

---

## 2. 목표 모듈 구조

```
hellogsm-server-26/
├── settings.gradle              (신규 — include 선언)
├── build.gradle                 (루트 — 공통 설정만, 플러그인은 apply false)
├── server/                      기존 src/ 이동. Spring Boot 4, Java 25
│   ├── build.gradle
│   └── src/
├── entrance-dsl/                Kotlin, JVM 21 — 순수, 의존성 없음
├── entrance-plans/              → entrance-dsl
├── entrance-engine/             → entrance-dsl        (DB 모름)
├── entrance-batch/              → entrance-engine + persistence  (신규)
└── entrance-lambda/             → entrance-engine     (JVM 21, 독립 배포, 신규)
```

### 불변 규칙 (통합 후에도 유지)

1. **`entrance-engine`은 `server`·`persistence`를 의존성으로 선언하지 않는다.** 이것이 "엔진은 DB를 모른다"를 컴파일 타임에 강제하는 유일한 장치다. 레포 분리와 동일한 강도이며, 위반 시 컴파일이 깨진다.
2. `entrance-plans`는 데이터만 — 로직 금지 (CLAUDE.md 원칙 1).
3. 엔진 계열 모듈의 **JVM target은 21 고정**. 서버가 25라고 따라 올리지 않는다 — Lambda 겸용 제약이다.
4. 패키지 루트는 그대로 둔다 — 서버 `team.themoment.hellogsmv3`, 엔진 `kr.hellogsm.entrance`. 통합했다고 통일할 이유가 없다.

---

## 3. 사전 조건

착수 전 확인할 것:

- [ ] **`hellogsm-server-26` 레포 수명 확인** — 이름의 `-26`이 학년도별 신규 레포를 뜻하는지 팀 확인. 매년 새 레포를 판다면 누적 자산인 `PlanXXXX.kt`가 매년 이사를 다녀야 하므로 **통합 판단 자체가 뒤집힌다.** 첫 커밋 2024-02, 패키지 `hellogsmv3`로 보아 여러 시즌을 한 레포로 넘겨온 것으로 보이나 확인이 필요하다. 되돌리기 비싼 유일한 축.
- [ ] 공유 MySQL 스키마 변경 주체 정책 (CONTEXT.md 미해결 질문 1)
- [ ] 통합 작업 기간 동안 서버 레포에 대규모 리팩터링 PR이 없을 것 — `src/` 전체 이동이라 충돌 시 해소 비용이 크다
- [ ] stage 환경에서 배포 검증이 가능한 상태

### 시점

**`entrance-batch` 구현보다 통합이 먼저다.** 배치는 서버의 영속성 레이어를 재사용해야 하는데, 통합 전에 만들면 자체 스키마 매핑을 짜게 되고 그게 바로 통합으로 없애려던 3중 매핑이다.

---

## 4. 단계별 계획

각 단계는 독립적으로 되돌릴 수 있고, 다음 단계로 넘어가기 전에 완료 기준을 만족해야 한다.

### M1 — 서버 레포 모듈화 (엔진 없이)

**엔진을 넣지 않고 구조 변경만 한다.** 파이프라인을 건드리는 유일한 단계이므로 격리해서 검증한다.

1. `settings.gradle` 신규 작성 — `rootProject.name = 'hellogsm-server-26'` + `include 'server'`
2. `src/`, `build.gradle`을 `server/`로 이동
3. 루트 `build.gradle` 신설 — `group`, `version`만 두고 플러그인은 전부 `apply false`
4. 아래 **5절 경로 의존 체크리스트**를 전부 반영
5. stage CD로 배포해 실제 기동 확인

**완료 기준**: `./gradlew clean build` 통과, stage 배포 후 헬스체크 정상, prod 파이프라인 dry-run 확인.

> M1이 실패하면 브랜치만 버리면 된다. 프로덕션은 영향받지 않는다.

### M2 — `every-entrance` 흡수

```bash
# 서버 레포에서 (every-entrance는 remote가 없으므로 로컬 경로를 그대로 쓴다)
git subtree add --prefix=entrance /Users/user/dev/every-entrance main
```

1. subtree로 히스토리째 삽입 (8커밋이라 비용 거의 없음)
2. `entrance/` 아래 3개 모듈을 루트로 승격하고 `settings.gradle`에 `include`
3. 루트 `build.gradle`에 Kotlin 플러그인 `apply false` 추가, 모듈별 toolchain 21 지정
4. 문서 정리 — `PLAN.md`·`CONTEXT.md`·`MIGRATION.md`는 `docs/entrance/`로, `CLAUDE.md` 규칙은 서버의 `.claude/rules/`로 편입
5. Gradle wrapper 버전 정리 — 서버 9.4.1 vs every-entrance 9.6.1. **서버 쪽 wrapper가 기준**이며, Kotlin 2.3.21 호환을 확인하고 필요 시 9.6.1로 올린다

**완료 기준**: 엔진 테스트 전부 통과(현재 상태 유지), 서버 빌드 무영향, CI에서 두 모듈군 모두 빌드됨.

### M3 — persistence 모듈 추출

`entrance-batch`가 서버 JPA 엔티티를 재사용할 수 있도록 영속성 레이어를 분리한다. Spring Boot 앱 리팩터링이라 규모가 있으므로 별도 단계로 둔다.

- 대상: `domain/oneseo/entity/*`(`Oneseo`, `EntranceTestResult`, `EntranceTestFactorsDetail`, `MiddleSchoolAchievement`, `OneseoPrivacyDetail`, `WantedScreeningChangeHistory`)와 대응 repository
- QueryDSL Q클래스 생성 경로가 모듈 이동에 영향받으므로 `options.generatedSourceOutputDirectory` 재확인 필요

> **이 단계를 건너뛰면 통합의 이득 절반을 포기하는 것**이다. 배치가 스키마를 다시 매핑하면 3중 매핑이 그대로 남는다. 건너뛸 거라면 그 사실을 알고 결정해야 한다.

### M4 — `entrance-batch` 구현

`go-hellogsm`을 대체하는 DB 러너. 엔진은 이미 전 범위 완료 상태이므로 엔진 입출력 ↔ 스키마 매핑과 잡 실행 골격이 작업 내용이다.
기준 커밋 주의: parity 기준은 `da09df4` **이전** 상수(정원 72명). 상세는 CLAUDE.md 주의사항.

### M5 — 서버가 엔진 소비 (PLAN.md Phase 3)

서버의 `LambdaScoreCalculatorClient`(Feign) 호출을 유지할지 in-process 엔진 호출로 바꿀지는 별도 판단이다.
**모의 성적 계산의 가용성 요건(server 다운 시에도 동작)은 유효하므로 `entrance-lambda`는 존속한다.** 레포 통합과 런타임 결합은 별개이며, 같은 레포의 모듈이어도 Lambda는 독립 배포된다.

---

## 5. 경로 의존 체크리스트 (M1 필수)

`src/` → `server/src/` 이동으로 깨지는 지점 전부. **누락 시 prod 배포가 조용히 실패한다.**

| 위치 | 현재 | 변경 후 |
|---|---|---|
| `.github/workflows/*-ci.yml` | `touch ./src/main/resources/application-prod.yml` | `./server/src/main/resources/...` |
| `.github/workflows/*-cd.yml` | 동일 (yml 생성) | 동일하게 `server/` 삽입 |
| `.github/workflows/*-cd.yml` | `cp build/libs/*.jar deploy/build/libs/` | `cp server/build/libs/*.jar ...` |
| `DockerfileProd` / `DockerfileStage` | `ARG JAR_FILE=build/libs/*.jar` | 경로 유지 시 CD의 zip 구성과 맞출 것 |
| `build.gradle` spotless | `target 'src/main/java/**/*.java'` | `server/build.gradle`로 이동 (모듈 상대경로) |
| `build.gradle` spotless | `groovyGradle { target '*.gradle' }` | 루트에서 `**/*.gradle`로 확장 |
| `build.gradle` | `jar { enabled = false }` | `server/build.gradle`에만 적용 |
| `build.gradle` | `configurations.configureEach { exclude group: 'org.bouncycastle' }` | `server/build.gradle`로 이동 — 루트에 두면 엔진 모듈까지 적용됨 |
| `build.gradle` | `compileJava.dependsOn spotlessApply` | `server/build.gradle` |
| `build.gradle` | `querydslSrcDir = "$projectDir/build/generated"` | `$projectDir`가 모듈 기준으로 바뀌므로 동작하나 재확인 |
| CI `setup-java` | JDK 25 단일 | 21·25 동시 설치 또는 foojay toolchain resolver 사용 |

### 주의: Spring Boot 플러그인 루트 적용 금지

`org.springframework.boot`·`io.spring.dependency-management`를 루트에 그대로 두면 모든 서브프로젝트에 전파돼 엔진 모듈의 의존성 해석까지 오염된다. 루트에서는 `apply false`로 선언만 하고 `server/build.gradle`에서만 적용한다.

### 주의: 툴체인 분리

```groovy
// 루트에 공통 toolchain을 두지 않는다
// server/build.gradle
java.toolchain { languageVersion = JavaLanguageVersion.of(25) }

// entrance-*/build.gradle.kts
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }
java { sourceCompatibility = JavaVersion.VERSION_21 }
```

CI가 JDK 25만 설치하므로 21 타깃 컴파일 경로를 확보해야 한다 — `setup-java`에 두 버전을 넣거나 foojay resolver로 자동 프로비저닝한다.

---

## 6. 롤백

- **M1 실패**: 브랜치 폐기. prod 무영향.
- **M2 이후 실패**: subtree 커밋 revert. 서버 코드는 M1 상태로 유지.
- **stage 배포 실패**: CD 워크플로 경로 수정 후 재시도. prod 워크플로는 M1 검증 완료까지 건드리지 않는다.

M1과 M2를 한 PR로 묶지 않는다 — 파이프라인 변경과 코드 유입이 섞이면 실패 원인 판별이 어려워진다.

---

## 7. 남은 판단

- `entrance-lambda`의 cold start 대응 — SnapStart 우선, 미달 시 GraalVM native (PLAN.md 9절 리스크)
- 실 Go 바이너리/배치 대비 재검증 및 병행 운전 (PLAN.md 7절 1·3항) — 통합과 독립적으로 진행 가능
- M3 persistence 추출 범위 — 전면 분리 vs 배치가 쓰는 엔티티만 최소 분리
