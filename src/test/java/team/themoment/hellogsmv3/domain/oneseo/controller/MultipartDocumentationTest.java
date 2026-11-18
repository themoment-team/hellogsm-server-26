package team.themoment.hellogsmv3.domain.oneseo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import team.themoment.hellogsmv3.global.thirdParty.aws.s3.controller.S3Controller;

@DisplayName("파일 업로드 문서 계약 테스트")
class MultipartDocumentationTest {
    @Nested
    @DisplayName("MultipartFile 업로드 엔드포인트는")
    class Describe_multipart {
        @ParameterizedTest
        @ValueSource(strings = {"uploadExcel", "uploadImage"})
        @DisplayName("JSON 기본값 대신 multipart를 선언하고 필수 file 파트를 유지한다")
        void it_declares_multipart(String name) throws Exception {
            Class<?> controller = name.equals("uploadExcel") ? OneseoController.class : S3Controller.class;
            var method = controller.getDeclaredMethod(name, MultipartFile.class);
            assertThat(method.getAnnotation(PostMapping.class).consumes())
                    .containsExactly(MediaType.MULTIPART_FORM_DATA_VALUE);
            var part = method.getParameters()[0].getAnnotation(RequestParam.class);
            assertThat(List.of(part.name(), part.value())).contains("file");
            assertThat(part.required()).isTrue();
        }
    }
}
