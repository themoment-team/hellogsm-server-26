package team.themoment.hellogsmv3.global.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("TimeBasedFilter 테스트")
class TimeBasedFilterTest {
    @Nested
    @DisplayName("doFilter 메서드는")
    class Describe_doFilter {
        @Nested
        @DisplayName("원서 수정 패턴에 기간 외 요청이 주어진 경우")
        class Context_outside_period {
            @Test
            @DisplayName("실제 회원 경로를 차단하고 공통 오류 형식으로 응답한다")
            void it_rejects_member_path_with_common_error() throws Exception {
                TimeBasedFilter filter = new TimeBasedFilter(JsonMapper.builder().build()).addFilter(HttpMethod.PUT,
                        "/oneseo/v3/oneseo/{memberId}",
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.of(2000, 2, 1, 0, 0));
                MockHttpServletResponse response = new MockHttpServletResponse();
                FilterChain chain = mock(FilterChain.class);

                filter.doFilter(new MockHttpServletRequest("PUT", "/oneseo/v3/oneseo/123"), response, chain);

                assertThat(response.getStatus()).isEqualTo(403);
                var body = JsonMapper.builder().build().readTree(response.getContentAsString());
                assertThat(body.get("status").asString()).isEqualTo("403 FORBIDDEN");
                assertThat(body.get("code").asInt()).isEqualTo(403);
                assertThat(body.get("message").asString()).contains("/oneseo/v3/oneseo/123:PUT");
                assertThat(body.has("data")).isFalse();
                then(chain).shouldHaveNoInteractions();
            }
        }
        @Nested
        @DisplayName("실제 보안 설정의 모든 요청 기간이 지난 경우")
        class Context_configured_periods_closed {
            private Filter configuredFilter() {
                LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0);
                LocalDateTime end = LocalDateTime.of(2000, 2, 1, 0, 0);
                ScheduleEnvironment schedule = new ScheduleEnvironment(start, end, end, start, start, end);
                return TimeBasedFilter.from(schedule, JsonMapper.builder().build());
            }

            @Test
            @DisplayName("지원자 본인과 관리자의 원서 수정은 기간 밖에도 다음 필터로 전달한다")
            void it_passes_oneseo_updates_outside_period() throws Exception {
                for (String path : new String[]{"/oneseo/v3/oneseo/me", "/oneseo/v3/oneseo/123"}) {
                    Filter filter = configuredFilter();
                    var request = new MockHttpServletRequest("PUT", path);
                    var response = new MockHttpServletResponse();
                    FilterChain chain = mock(FilterChain.class);

                    filter.doFilter(request, response, chain);

                    then(chain).should().doFilter(request, response);
                    assertThat(response.getStatus()).as(path).isEqualTo(200);
                    assertThat(response.getContentAsString()).isEmpty();
                }
            }

            @Test
            @DisplayName("원서 접수와 엑셀 업로드 기간 제한 요청은 차단한다")
            void it_rejects_all_configured_restricted_routes() throws Exception {
                for (String[] route : new String[][]{{"POST", "/oneseo/v3/oneseo/me"},
                        {"POST", "/oneseo/v3/temp-storage"}, {"POST", "/oneseo/v3/image"},
                        {"POST", "/oneseo/v3/excel"}}) {
                    Filter filter = configuredFilter();
                    var request = new MockHttpServletRequest(route[0], route[1]);
                    var response = new MockHttpServletResponse();
                    FilterChain chain = mock(FilterChain.class);

                    filter.doFilter(request, response, chain);

                    assertThat(response.getStatus()).as("%s %s", route[0], route[1]).isEqualTo(403);
                    then(chain).shouldHaveNoInteractions();
                }
            }
        }

        @Nested
        @DisplayName("기간 내 요청 또는 다른 메서드와 경로가 주어진 경우")
        class Context_allowed {
            @Test
            @DisplayName("요청을 다음 필터로 전달한다")
            void it_passes_matching_open_and_unmatched_requests() throws Exception {
                TimeBasedFilter filter = new TimeBasedFilter(JsonMapper.builder().build()).addFilter(HttpMethod.PUT,
                        "/oneseo/v3/oneseo/{memberId}",
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.of(2100, 1, 1, 0, 0));
                for (String[] route : new String[][]{{"PUT", "/oneseo/v3/oneseo/123"}, {"GET", "/oneseo/v3/oneseo/123"},
                        {"PUT", "/other/123"}, {"PUT", "/oneseo/v3/oneseo/123/approval"}}) {
                    var request = new MockHttpServletRequest(route[0], route[1]);
                    var response = new MockHttpServletResponse();
                    FilterChain chain = mock(FilterChain.class);
                    filter.doFilter(request, response, chain);
                    then(chain).should().doFilter(request, response);
                }
            }
        }

        @Nested
        @DisplayName("여러 규칙이 같은 요청과 일치하는 경우")
        class Context_overlapping_rules {
            @Test
            @DisplayName("먼저 등록한 규칙의 기간을 적용한다")
            void it_applies_first_registered_rule() throws Exception {
                TimeBasedFilter filter = new TimeBasedFilter(JsonMapper.builder().build()).addFilter(HttpMethod.POST,
                        "/oneseo/v3/oneseo/me",
                        LocalDateTime.of(2000, 1, 1, 0, 0),
                        LocalDateTime.of(2100, 1, 1, 0, 0)).addFilter(HttpMethod.POST,
                                "/oneseo/v3/oneseo/**",
                                LocalDateTime.of(2000, 1, 1, 0, 0),
                                LocalDateTime.of(2000, 2, 1, 0, 0));
                var request = new MockHttpServletRequest("POST", "/oneseo/v3/oneseo/me");
                var response = new MockHttpServletResponse();
                FilterChain chain = mock(FilterChain.class);

                filter.doFilter(request, response, chain);

                then(chain).should().doFilter(request, response);
            }
        }
    }
}
