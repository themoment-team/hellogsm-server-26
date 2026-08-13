package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractionMetaResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.MiddleSchoolAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.AchievementTextExtractor;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser;
import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("생활기록부 성적 추출 서비스 테스트")
class ExtractMiddleSchoolAchievementServiceTest {

    private static final Long MEMBER_ID = 1L;
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    @Mock
    private AchievementTextExtractor textExtractor;

    @Mock
    private MiddleSchoolRecordParser parser;

    private ExtractMiddleSchoolAchievementService service;

    @BeforeEach
    void setUp() {
        service = new ExtractMiddleSchoolAchievementService(List.of(textExtractor), parser);
        given(textExtractor.supports(PDF_CONTENT_TYPE)).willReturn(true);
    }

    private MockMultipartFile pdfFile() {
        return new MockMultipartFile("file", "record.pdf", PDF_CONTENT_TYPE, "content".getBytes());
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("검정고시 지원자가 요청한 경우")
        class Context_with_ged_applicant {

            @Test
            @DisplayName("ExpectedException을 던지고 파일을 읽지 않는다")
            void it_throws_expected_exception() {
                assertThrows(ExpectedException.class,
                        () -> service.execute(pdfFile(), GraduationType.GED, "자유학년제", MEMBER_ID));

                verify(textExtractor, never()).extract(any(), any());
            }
        }

        @Nested
        @DisplayName("빈 파일이 주어진 경우")
        class Context_with_empty_file {

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                MockMultipartFile emptyFile = new MockMultipartFile("file",
                        "record.pdf",
                        PDF_CONTENT_TYPE,
                        new byte[0]);

                assertThrows(ExpectedException.class,
                        () -> service.execute(emptyFile, GraduationType.CANDIDATE, "자유학년제", MEMBER_ID));
            }
        }

        @Nested
        @DisplayName("지원하지 않는 형식의 파일이 주어진 경우")
        class Context_with_unsupported_content_type {

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                MockMultipartFile imageFile = new MockMultipartFile("file",
                        "record.png",
                        "image/png",
                        "content".getBytes());

                assertThrows(ExpectedException.class,
                        () -> service.execute(imageFile, GraduationType.CANDIDATE, "자유학년제", MEMBER_ID));
            }
        }

        @Nested
        @DisplayName("텍스트 레이어가 없는 스캔 PDF가 주어진 경우")
        class Context_with_scanned_pdf {

            @BeforeEach
            void setUp() {
                given(textExtractor.extract(any(), any()))
                        .willReturn(new ExtractedTextDto("", 4, false, ExtractionSource.TEXT_LAYER, 1.0));
            }

            @Test
            @DisplayName("ExpectedException을 던지고 파싱하지 않는다")
            void it_throws_expected_exception() {
                assertThrows(ExpectedException.class,
                        () -> service.execute(pdfFile(), GraduationType.CANDIDATE, "자유학년제", MEMBER_ID));

                verify(parser, never()).parse(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("텍스트 레이어가 있는 PDF가 주어진 경우")
        class Context_with_text_layer_pdf {

            @BeforeEach
            void setUp() {
                given(textExtractor.extract(any(), any())).willReturn(
                        new ExtractedTextDto("교과학습발달상황".repeat(30), 4, true, ExtractionSource.TEXT_LAYER, 1.0));
                given(parser.parse(any(), any(), any())).willReturn(ExtractedAchievementResDto.builder()
                        .achievement(MiddleSchoolAchievementResDto.builder().build())
                        .meta(ExtractionMetaResDto.builder().confidence(BigDecimal.ONE).hasTextLayer(true).pageCount(4)
                                .unrecognizedSubjects(List.of()).missingSemesters(List.of()).warnings(List.of())
                                .build())
                        .build());
            }

            @Test
            @DisplayName("파서에 추출된 텍스트를 전달한다")
            void it_delegates_to_parser() {
                service.execute(pdfFile(), GraduationType.CANDIDATE, "자유학년제", MEMBER_ID);

                verify(parser).parse(any(ExtractedTextDto.class), any(GraduationType.class), any());
            }

            @Test
            @DisplayName("추출 원문을 응답에 포함하지 않는다")
            void it_does_not_expose_raw_text() {
                ExtractedAchievementResDto result = service
                        .execute(pdfFile(), GraduationType.CANDIDATE, "자유학년제", MEMBER_ID);

                assertThat(result.meta().rawText()).isNull();
            }
        }
    }
}
