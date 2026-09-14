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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ExtractMiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractionMetaResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.MiddleSchoolAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser;
import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("생활기록부 성적 추출 서비스 테스트")
class ExtractMiddleSchoolAchievementServiceTest {

    private static final Long MEMBER_ID = 1L;

    /** 최소 글자 수(200자)를 넘기는 임의의 본문입니다. */
    private static final String ENOUGH_TEXT = "교과학습발달상황".repeat(30);

    @Mock
    private MiddleSchoolRecordParser parser;

    private ExtractMiddleSchoolAchievementService service;

    @BeforeEach
    void setUp() {
        service = new ExtractMiddleSchoolAchievementService(parser);
    }

    private ExtractMiddleSchoolAchievementReqDto request(String rawText,
            GraduationType graduationType,
            ExtractionSource source,
            Double recognitionConfidence) {
        return new ExtractMiddleSchoolAchievementReqDto(rawText,
                17,
                source == ExtractionSource.TEXT_LAYER,
                source,
                recognitionConfidence,
                graduationType,
                "자유학년제");
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        @Nested
        @DisplayName("검정고시 지원자가 요청한 경우")
        class Context_with_ged_applicant {

            @Test
            @DisplayName("ExpectedException을 던지고 파싱하지 않는다")
            void it_throws_expected_exception() {
                assertThrows(ExpectedException.class,
                        () -> service.execute(
                                request(ENOUGH_TEXT, GraduationType.GED, ExtractionSource.TEXT_LAYER, null),
                                MEMBER_ID));

                verify(parser, never()).parse(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("졸업예정자인데 자유학기제 구분이 없는 경우")
        class Context_without_liberal_system {

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExtractMiddleSchoolAchievementReqDto reqDto = new ExtractMiddleSchoolAchievementReqDto(ENOUGH_TEXT,
                        17,
                        true,
                        ExtractionSource.TEXT_LAYER,
                        null,
                        GraduationType.CANDIDATE,
                        null);

                assertThrows(ExpectedException.class, () -> service.execute(reqDto, MEMBER_ID));
            }
        }

        @Nested
        @DisplayName("텍스트가 최소 길이에 못 미치는 경우")
        class Context_with_too_short_text {

            @Test
            @DisplayName("ExpectedException을 던지고 파싱하지 않는다")
            void it_throws_expected_exception() {
                assertThrows(ExpectedException.class,
                        () -> service.execute(request("성적표", GraduationType.CANDIDATE, ExtractionSource.OCR, 0.78),
                                MEMBER_ID));

                verify(parser, never()).parse(any(), any(), any());
            }
        }

        @Nested
        @DisplayName("충분한 길이의 텍스트가 주어진 경우")
        class Context_with_enough_text {

            @BeforeEach
            void setUp() {
                given(parser.parse(any(), any(), any())).willReturn(ExtractedAchievementResDto.builder()
                        .achievement(MiddleSchoolAchievementResDto.builder().build())
                        .meta(ExtractionMetaResDto.builder().confidence(BigDecimal.ONE).hasTextLayer(true).pageCount(17)
                                .unrecognizedSubjects(List.of()).missingSemesters(List.of()).warnings(List.of())
                                .build())
                        .build());
            }

            @Test
            @DisplayName("전달받은 텍스트를 그대로 파서에 넘긴다")
            void it_delegates_to_parser() {
                service.execute(request(ENOUGH_TEXT, GraduationType.CANDIDATE, ExtractionSource.OCR, 0.78), MEMBER_ID);

                ArgumentCaptor<ExtractedTextDto> captor = ArgumentCaptor.forClass(ExtractedTextDto.class);
                verify(parser).parse(captor.capture(), any(GraduationType.class), any());
                assertThat(captor.getValue().rawText()).isEqualTo(ENOUGH_TEXT);
                assertThat(captor.getValue().source()).isEqualTo(ExtractionSource.OCR);
                assertThat(captor.getValue().recognitionConfidence()).isEqualTo(0.78);
            }

            @Test
            @DisplayName("인식 신뢰도가 없으면 텍스트 레이어는 1.0으로 본다")
            void it_defaults_text_layer_confidence() {
                service.execute(request(ENOUGH_TEXT, GraduationType.CANDIDATE, ExtractionSource.TEXT_LAYER, null),
                        MEMBER_ID);

                ArgumentCaptor<ExtractedTextDto> captor = ArgumentCaptor.forClass(ExtractedTextDto.class);
                verify(parser).parse(captor.capture(), any(GraduationType.class), any());
                assertThat(captor.getValue().recognitionConfidence()).isEqualTo(1.0);
            }

            @Test
            @DisplayName("인식 신뢰도가 없으면 OCR은 1.0보다 낮게 본다")
            void it_defaults_ocr_confidence_below_one() {
                service.execute(request(ENOUGH_TEXT, GraduationType.CANDIDATE, ExtractionSource.OCR, null), MEMBER_ID);

                ArgumentCaptor<ExtractedTextDto> captor = ArgumentCaptor.forClass(ExtractedTextDto.class);
                verify(parser).parse(captor.capture(), any(GraduationType.class), any());
                assertThat(captor.getValue().recognitionConfidence()).isLessThan(1.0);
            }

            @Test
            @DisplayName("전달받은 원문을 응답에 되돌려주지 않는다")
            void it_does_not_expose_raw_text() {
                ExtractedAchievementResDto result = service
                        .execute(request(ENOUGH_TEXT, GraduationType.CANDIDATE, ExtractionSource.OCR, 0.78), MEMBER_ID);

                assertThat(result.meta().rawText()).isNull();
            }
        }
    }
}
