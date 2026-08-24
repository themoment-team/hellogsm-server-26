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
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocTable;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocTableCell;
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
                KordocTable table = new KordocTable(List.of(List.of(semesterCell, subjectCell, scoreCell)));
                KordocBlock tableBlock = new KordocBlock("table", null, table, 1);

                KordocParseResult parseResult = new KordocParseResult(List.of(heading, tableBlock));

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
                KordocTable table = new KordocTable(List.of(List.of(subjectCell, scoreCell)));
                KordocBlock tableBlock = new KordocBlock("table", null, table, 1);

                KordocConversionResult conversion = converter.convert(new KordocParseResult(List.of(tableBlock)));

                assertThat(conversion.unrecognizedSubjectBlobs()).containsExactly("국어한문수학");
            }
        }

        @Nested
        @DisplayName("교과(대분류)와 과목(실제 과목명)이 별도 열로 나뉜 표가 주어진 경우")
        class Context_with_separate_category_and_subject_columns {

            @Test
            @DisplayName("한글이 더 많은 대분류 열이 아니라 실제로 토큰화에 성공하는 과목 열을 사용한다")
            void it_picks_the_column_that_actually_tokenizes() {
                // 실제 kordoc 출력에서 확인된 형태: rowSpan 병합 셀이 줄바꿈 없이 반복되어 "1"이 "111111"로,
                // "교과" 열은 "(역사포함)" 같은 부가 설명과 "/" 구분자가 섞인 채로 재구성됩니다.
                KordocTableCell semesterCell = new KordocTableCell("111111", 1, 1);
                KordocTableCell categoryCell = new KordocTableCell(String
                        .join("\n", "사회(역사포함)/도덕", "사회(역사포함)/도덕수학", "과학/기술·가정/정보", "과학/기술·가정/정보영어"), 1, 1);
                KordocTableCell subjectCell = new KordocTableCell(String.join("\n", "사회도덕수학과학", "기술·가정영어"), 1, 1);
                KordocTableCell scoreCell = new KordocTableCell(String.join("\n", "P", "P", "P", "P", "P", "P"), 1, 1);
                KordocTable table = new KordocTable(
                        List.of(List.of(semesterCell, categoryCell, subjectCell, scoreCell)));
                KordocBlock tableBlock = new KordocBlock("table", null, table, 1);

                KordocConversionResult conversion = converter.convert(new KordocParseResult(List.of(tableBlock)));

                assertThat(conversion.unrecognizedSubjectBlobs()).isEmpty();
                assertThat(conversion.rawText().lines())
                        .contains("1", "사회 P", "도덕 P", "수학 P", "과학 P", "기술가정 P", "영어 P");
            }
        }

        @Nested
        @DisplayName("원점수/성취도 열을 찾을 수 없는 행(출결 등으로 추정)이 주어진 경우")
        class Context_with_non_achievement_row {

            @Test
            @DisplayName("셀을 그대로 이어붙인 줄로 지나간다")
            void it_passes_through_joined_cells() {
                KordocTable table = new KordocTable(
                        List.of(List.of(new KordocTableCell("1", 1, 1), new KordocTableCell("190", 1, 1))));
                KordocBlock tableBlock = new KordocBlock("table", null, table, 1);

                KordocConversionResult conversion = converter.convert(new KordocParseResult(List.of(tableBlock)));

                assertThat(conversion.rawText()).contains("1 190");
            }
        }

        @Nested
        @DisplayName("학년을 언급할 뿐인 제목 블록이 주어진 경우")
        class Context_with_heading_merely_mentioning_grade {

            @Test
            @DisplayName("학년 표시로 오인하지 않는다")
            void it_does_not_misread_as_grade_marker() {
                KordocBlock title = new KordocBlock("heading", "2024학년도 생활기록부", null, 1);
                KordocBlock caption = new KordocBlock("text", "학년별 출결현황", null, 1);

                KordocConversionResult conversion = converter.convert(new KordocParseResult(List.of(title, caption)));

                assertThat(conversion.rawText()).doesNotContain("[").doesNotContain("학년]");
            }
        }
    }
}
