# Testing Rules — hellogsm-server-25

> Scope: Java/Spring modules (`server`, `persistence`) only. The Kotlin `entrance-*` modules use `kotlin.test` + JUnit5 with backtick Korean test names (not the Describe/Context/It nesting below) — see [`entrance/CLAUDE.md`](../../entrance/CLAUDE.md).

## Framework
- JUnit 5 (Jupiter)
- Mockito 5 (inline mock maker)
- BDD-style assertions and stubbing

## Test Class Structure
Use the **Describe / Context / It** nested pattern:

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("{Feature} {Subject} 테스트")
class ActionFeatureServiceTest {

    @Mock
    private DependencyRepository dependencyRepository;

    @InjectMocks
    private ActionFeatureService service;

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 입력이 주어진 경우")
        class Context_with_valid_input {

            @BeforeEach
            void setUp() {
                given(dependencyRepository.findById(1L)).willReturn(Optional.of(entity));
            }

            @Test
            @DisplayName("결과를 반환한다")
            void it_returns_result() {
                // arrange / act / assert
            }
        }

        @Nested
        @DisplayName("존재하지 않는 ID가 주어진 경우")
        class Context_with_nonexistent_id {

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                assertThrows(ExpectedException.class, () -> service.execute(99L));
            }
        }
    }
}
```

## Naming Conventions

| Part          | Pattern                                  |
|---------------|------------------------------------------|
| Test class    | `{ClassUnderTest}Test`                   |
| Outer class   | `@DisplayName("{Subject} 테스트")`       |
| Nested Describe | `Describe_{methodName}`               |
| Nested Context  | `Context_{condition}`                 |
| Test method   | `it_{expected_behavior}()`               |

## Mockito Initialization
- Prefer `@ExtendWith(MockitoExtension.class)` for JUnit 5 unit tests
- Do not call `MockitoAnnotations.openMocks(this)` in new tests
- Keep a top-level `@BeforeEach` only when the test needs additional setup beyond mock initialization

## Stubbing Style — BDD (Preferred)
```java
// CORRECT — BDD style
given(repo.findById(1L)).willReturn(Optional.of(entity));
given(repo.existsById(1L)).willReturn(true);

// AVOID — Mockito classic style (only if BDD import unavailable)
when(repo.findById(1L)).thenReturn(Optional.of(entity));
```

## Verification
```java
verify(repo).save(any(Entity.class));
verify(repo, never()).delete(any());
```

## Inline Mock Creation
```java
// Use mock() for one-off objects in test methods
Member member = mock(Member.class);
given(member.getId()).willReturn(1L);
```

## Exception Testing
```java
assertThrows(ExpectedException.class, () -> service.execute(invalidId));
```

## Source of Truth
**Service/business logic code is the source of truth — tests must match it.**
When a test fails because the service was correctly updated, fix the test, not the service.

## What to Test
- Service classes are the primary test target
- Controller tests are optional (covered by integration tests)
- Repository custom queries: test with real DB in integration tests, not unit tests

## File Location
```
src/test/java/team/themoment/hellogsmv3/domain/{feature}/service/{ServiceName}Test.java
```

## Prohibited Patterns
- No `@SpringBootTest` for unit tests — use `@InjectMocks` + `@Mock`
- No mocking of entity fields unless necessary — use builders
- No testing internal implementation details — test observable behavior
- No `Thread.sleep()` in unit tests
- Do not fix a failing test by weakening the assertion — find the root cause
