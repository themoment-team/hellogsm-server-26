package team.themoment.hellogsmv3.domain.oneseo.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;
import team.themoment.hellogsmv3.global.exception.GlobalExceptionHandler;
import team.themoment.sdk.config.ResponseProperties;
import team.themoment.sdk.response.ApiResponseWrapper;

@DisplayName("원서 HTTP 계약 회귀 테스트")
class OneseoHttpContractTest {
    private MockMvc mvc;
    private Object[] services;

    @BeforeEach
    void setUp() throws Exception {
        var constructor = OneseoController.class.getConstructors()[0];
        services = Arrays.stream(constructor.getParameterTypes()).map(type -> mock(type)).toArray();
        var controller = constructor.newInstance(services);
        var validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        var proxy = new ProxyFactory(controller);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new MethodValidationInterceptor((jakarta.validation.Validator) validator));
        mvc = MockMvcBuilders.standaloneSetup(proxy.getProxy()).setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler(), new ApiResponseWrapper(new ResponseProperties()))
                .setCustomArgumentResolvers(new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return parameter.hasParameterAnnotation(AuthRequest.class);
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter,
                            ModelAndViewContainer container,
                            NativeWebRequest request,
                            WebDataBinderFactory binderFactory) {
                        return 1L;
                    }
                }).build();
    }

    @Nested
    @DisplayName("search 메서드는")
    class Describe_search {
        @Nested
        @DisplayName("잘못된 페이지 입력인 경우")
        class Context_invalid_pagination {
            @ParameterizedTest
            @CsvSource({"0, 0", "0, -1", "-1, 10"})
            @DisplayName("SDK 래퍼를 거쳐 HTTP 400을 반환하고 서비스를 호출하지 않는다")
            void it_returns_bad_request(int page, int size) throws Exception {
                mvc.perform(get("/oneseo/v3/oneseo/search").param("page", String.valueOf(page))
                        .param("size", String.valueOf(size)).param("testResultTag", "ALL"))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));
                verifyNoInteractions(services);
            }
        }
    }

    @Nested
    @DisplayName("원서 등록·수정 메서드는")
    class Describe_write {
        @Nested
        @DisplayName("성적 객체가 없는 경우")
        class Context_missing_achievement {
            @ParameterizedTest
            @CsvSource({"POST, /oneseo/v3/oneseo/me, CANDIDATE", "PUT, /oneseo/v3/oneseo/1, CANDIDATE",
                    "PUT, /oneseo/v3/oneseo/me, CANDIDATE", "POST, /oneseo/v3/oneseo/me, GED",
                    "PUT, /oneseo/v3/oneseo/1, GED", "PUT, /oneseo/v3/oneseo/me, GED",
                    "POST, /oneseo/v3/oneseo/me, GRADUATE", "PUT, /oneseo/v3/oneseo/1, GRADUATE",
                    "PUT, /oneseo/v3/oneseo/me, GRADUATE"})
            @DisplayName("모든 졸업 구분과 쓰기 경로에서 HTTP 400을 반환한다")
            void it_returns_bad_request(String method, String path, String graduationType) throws Exception {
                var request = method.equals("POST") ? post(path) : put(path);
                mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(body(graduationType, "null")))
                        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400))
                        .andExpect(jsonPath("$.message")
                                .value(org.hamcrest.Matchers.containsString("middleSchoolAchievement")));
                verifyNoInteractions(services);
            }
        }

        @Nested
        @DisplayName("정상 등록 요청인 경우")
        class Context_valid_creation {
            @Test
            @DisplayName("문서와 같은 HTTP 201을 반환한다")
            void it_returns_created() throws Exception {
                mvc.perform(post("/oneseo/v3/oneseo/me").contentType(MediaType.APPLICATION_JSON)
                        .content(body("GED", "{\"gedAvgScore\":90}"))).andExpect(status().isCreated())
                        .andExpect(jsonPath("$.code").value(201));
            }

            @Test
            @DisplayName("부분 임시저장은 성적 없이 HTTP 200이다")
            void it_accepts_partial_temp_storage() throws Exception {
                mvc.perform(post("/oneseo/v3/temp-storage").param("step", "1").contentType(MediaType.APPLICATION_JSON)
                        .content("{}")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
            }
        }
    }

    @Nested
    @DisplayName("컨트롤러 문서는")
    class Describe_documentation {
        @Nested
        @DisplayName("보호된 전체 API인 경우")
        class Context_all_operations {
            @Test
            @DisplayName("모든 operation에 성공·인증 오류와 권한 설명이 있다")
            void it_documents_every_operation() {
                var methods = List.of(OneseoController.class, OneseoExtractionController.class).stream()
                        .flatMap(type -> Arrays.stream(type.getDeclaredMethods()))
                        .filter(method -> method.isAnnotationPresent(Operation.class)).toList();
                assertEquals(22, methods.size());
                for (var method : methods) {
                    assertTrue(method.getAnnotation(Operation.class).description().contains("권한:"), method.getName());
                    var responses = method.getAnnotation(ApiResponses.class);
                    assertNotNull(responses, method.getName());
                    var codes = Arrays.stream(responses.value()).map(response -> response.responseCode()).toList();
                    assertTrue(codes.contains(method.getName().equals("create") ? "201" : "200"), method.getName());
                    assertTrue(codes.containsAll(List.of("401", "403")), method.getName());
                }
            }
        }
    }

    private static String body(String graduationType, String achievement) {
        return """
                {"guardianName":"김보호","guardianPhoneNumber":"01012345678","relationshipWithGuardian":"모",
                 "profileImg":"https://example.com/photo.jpg","address":"광주광역시","detailAddress":"101호",
                 "graduationType":"%s","graduationDate":"2027-02","firstDesiredMajor":"SW",
                 "secondDesiredMajor":"AI","thirdDesiredMajor":"IOT","screening":"GENERAL",
                 "middleSchoolAchievement":%s}
                """.formatted(graduationType, achievement);
    }
}
