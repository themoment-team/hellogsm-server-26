package team.themoment.hellogsmv3.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import team.themoment.sdk.config.ResponseProperties;
import team.themoment.sdk.response.ApiResponseWrapper;

@DisplayName("Swagger YAML 응답 설정 테스트")
class SwaggerResponsePropertiesTest {
    @Nested
    @DisplayName("not-wrapping-urls 설정은")
    class Describe_notWrappingUrls {
        @Nested
        @DisplayName("실제 SDK에 YAML 문서가 전달된 경우")
        class Context_yaml_document {
            @Test
            @DisplayName("기본 문서와 그룹 문서의 바이트를 그대로 유지한다")
            void it_preserves_yaml_bytes() throws Exception {
                var properties = new YamlPropertySourceLoader()
                        .load("application", new ClassPathResource("application.yml")).getFirst();
                List<String> urls = new ArrayList<>();
                for (int i = 0; properties.getProperty("sdk.response.not-wrapping-urls[" + i + "]") != null; i++) {
                    urls.add(properties.getProperty("sdk.response.not-wrapping-urls[" + i + "]").toString());
                }
                assertThat(urls).contains("/api-docs.yaml", "/api-docs.yaml/**");
                assertThat(properties.getProperty("springdoc.override-with-generic-response")).isEqualTo(false);
                ApiResponseWrapper wrapper = new ApiResponseWrapper(new ResponseProperties(true, urls));
                byte[] yaml = "openapi: 3.1.0".getBytes(StandardCharsets.UTF_8);
                for (String path : List.of("/api-docs.yaml", "/api-docs.yaml/Hello%2C%20GSM%202025%20API")) {
                    Object result = wrapper.beforeBodyWrite(yaml,
                            new MethodParameter(SwaggerResponsePropertiesTest.class.getDeclaredMethod("yaml"), -1),
                            MediaType.parseMediaType("application/yaml"),
                            ByteArrayHttpMessageConverter.class,
                            new ServletServerHttpRequest(new MockHttpServletRequest("GET", path)),
                            new ServletServerHttpResponse(new MockHttpServletResponse()));
                    assertThat(result).isSameAs(yaml);
                }
            }
        }
    }

    public byte[] yaml() {
        return null;
    }
}
