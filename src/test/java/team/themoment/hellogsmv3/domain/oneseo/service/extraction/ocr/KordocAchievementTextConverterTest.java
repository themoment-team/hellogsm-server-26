package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocBlock;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocParseResult;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocTableCell;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocTableRow;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.SubjectNameNormalizer;

/**
 * 합성(synthetic) kordoc JSON 형태로 검증합니다. 실제 kordoc이 이 환경에 설치되어 있지 않아 실제 출력으로는
 * 검증하지 못했습니다 — 실제 kordoc 환경에서 RecordExtractionDiagnosticTest로 별도 검증이 필요합니다.
 */
@DisplayName("kordoc 표 재구성 결과 변환 테스트")
class KordocAchievementTextConverterTest {

    private KordocAchievementTextConverter converter;

    @BeforeEach
    void setUp() {
        converter = new KordocAchievementTextConverter(new KordocSubjectTokenizer());
    }

    @Nested
    @DisplayName("convert 메서드는")
    class Describe_convert {

        @Nested
        @DisplayName("학년 제목과 과목명이 붙어있는 성적 표가 주어진 경우")
        class Context_with_grade_heading_and_merged_subject_table {

            @Test
            @DisplayName("기존 파서가 읽을 수 있는 rawText로 변환한다")
            void it_converts_to_parser_readable_text() {
                KordocBlock heading = new KordocBlock("heading", "1학년", null, 1);

                KordocTableCell semesterCell = new KordocTableCell("2", 1, 1);
                KordocTableCell subjectCell = new KordocTableCell("국어사회도덕역사수학과학기술가정정보영어", 1, 1);
                KordocTableCell scoreCell = new KordocTableCell(String.join("\n",
                        "91/77.8 A(168)",
                        "88/70.2 B(168)",
                        "75/65.0 C(168)",
                        "60/64.0 D(168)",
                        "95/71.0 A(168)",
                        "85/69.0 B(168)",
                        "78/68.0 C(168)",
                        "65/64.0 D(168)",
                        "90/72.0 A(168)"), 1, 1);
                KordocTableRow row = new KordocTableRow(List.of(semesterCell, subjectCell, scoreCell));
                KordocBlock table = new KordocBlock("table", null, List.of(row), 1);

                KordocParseResult parseResult = new KordocParseResult(List.of(heading, table));

                KordocConversionResult conversion = converter.convert(parseResult);
                assertThat(conversion.unrecognizedSubjectBlobs()).isEmpty();

                ExtractedAchievementResDto result = new MiddleSchoolRecordParser(new SubjectNameNormalizer()).parse(
                        new ExtractedTextDto(conversion.rawText(), 1, false, ExtractionSource.OCR, 0.8),
                        GraduationType.GRADUATE,
                        null);

                assertThat(result.achievement().achievement1_2()).containsExactly(5, 4, 3, 2, 5, 4, 3, 2, 5);
            }
        }

        @Nested
        @DisplayName("과목명 열이 표준 과목 사전만으로 분해되지 않는 경우")
        class Context_with_unsegmentable_subject_blob {

            @Test
            @DisplayName("그 행을 건너뛰고 원문을 unrecognizedSubjectBlobs로 돌려준다")
            void it_skips_row_and_reports_blob() {
                KordocTableCell subjectCell = new KordocTableCell("국어한문수학", 1, 1);
                KordocTableCell scoreCell = new KordocTableCell(String
                        .join("\n", "91/77.8 A(168)", "88/70.2 B(168)", "75/65.0 C(168)"), 1, 1);
                KordocTableRow row = new KordocTableRow(List.of(subjectCell, scoreCell));
                KordocBlock table = new KordocBlock("table", null, List.of(row), 1);

                KordocConversionResult conversion = converter.convert(new KordocParseResult(List.of(table)));

                assertThat(conversion.unrecognizedSubjectBlobs()).containsExactly("국어한문수학");
            }
        }

        @Nested
        @DisplayName("원점수/성취도 열을 찾을 수 없는 행(출결 등으로 추정)이 주어진 경우")
        class Context_with_non_achievement_row {

            @Test
            @DisplayName("셀을 그대로 이어붙인 줄로 지나간다")
            void it_passes_through_joined_cells() {
                KordocTableRow row = new KordocTableRow(
                        List.of(new KordocTableCell("1", 1, 1), new KordocTableCell("190", 1, 1)));
                KordocBlock table = new KordocBlock("table", null, List.of(row), 1);

                KordocConversionResult conversion = converter.convert(new KordocParseResult(List.of(table)));

                assertThat(conversion.rawText()).contains("1 190");
            }
        }
    }
}
