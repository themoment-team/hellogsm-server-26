package team.themoment.hellogsmv3.domain.oneseo.service.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractionWarningType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;

/**
 * 파서가 기대하는 생활기록부 텍스트 형식과, 프론트엔드와 맞춘 배열 인덱싱 규칙을 고정하는 테스트입니다.
 *
 * <p>
 * 실제 생활기록부 샘플을 확보하면 SAMPLE_TEXT를 실제 추출 원문으로 교체하고, 그에 맞추어 파서의 정규식을 조정하면 됩니다.
 */
@DisplayName("생활기록부 성적 파서 테스트")
class MiddleSchoolRecordParserTest {

    private static final String SAMPLE_TEXT = """
            교과학습발달상황
            [1학년]
            학기 교과 과목 원점수/과목평균(표준편차) 성취도(수강자수) 비고
            2 국어 국어 85/70.5(12.3) A(250)
            2 사회(역사포함) 사회 78/65.2(14.1) B(250)
            2 수학 수학 91/68.0(15.0) A(250)
            2 기술·가정 기술·가정 82/70.0(12.0) B(250)
            2 정보 정보 88/70.0(12.0) A(250)
            2 한문 한문 79/68.0(13.0) B(250)
            2 체육 체육 A
            [2학년]
            학기 교과 과목 원점수/과목평균(표준편차) 성취도(수강자수) 비고
            1 국어 국어 88/71.2(11.9) A(248)
            1 사회(역사포함) 사회 70/66.0(13.0) C(248)
            1 수학 수학 60/64.0(16.0) D(248)
            1 체육 체육 A
            1 예술 음악 B
            1 예술 미술 C
            2 국어 국어 80/70.0(12.0) B(248)
            2 사회(역사포함) 사회 75/66.5(13.5) B(248)
            2 수학 수학 95/69.0(14.5) A(248)
            [3학년]
            학기 교과 과목 원점수/과목평균(표준편차) 성취도(수강자수) 비고
            1 국어 국어 92/72.0(11.0) A(245)
            1 사회(역사포함) 사회 84/67.0(12.5) B(245)
            1 수학 수학 77/65.0(15.5) C(245)
            1 체육 체육 B
            출결상황
            1 190 2 0 0 1 0 0 0 0 0 0 0 0
            창의적 체험활동상황
            1 봉사활동 7
            """;

    private MiddleSchoolRecordParser parser;

    @BeforeEach
    void setUp() {
        parser = new MiddleSchoolRecordParser(new SubjectNameNormalizer());
    }

    private ExtractedAchievementResDto parseSample(GraduationType graduationType, String liberalSystem) {
        return parser.parse(new ExtractedTextDto(SAMPLE_TEXT, 4, true, ExtractionSource.TEXT_LAYER, 1.0),
                graduationType,
                liberalSystem);
    }

    @Nested
    @DisplayName("parse 메서드는")
    class Describe_parse {

        @Nested
        @DisplayName("자유학년제 졸업예정자의 생활기록부가 주어진 경우")
        class Context_with_free_year_candidate {

            private ExtractedAchievementResDto result;

            @BeforeEach
            void setUp() {
                result = parseSample(GraduationType.CANDIDATE, MiddleSchoolRecordParser.FREE_YEAR_SYSTEM);
            }

            @Test
            @DisplayName("표준 과목 9개를 프론트엔드와 동일한 순서로 항상 반환한다")
            void it_always_returns_standard_subjects_in_fixed_order() {
                assertThat(result.achievement().generalSubjects())
                        .containsExactly("국어", "사회", "도덕", "역사", "수학", "과학", "기술가정", "정보", "영어");
            }

            @Test
            @DisplayName("표준 목록에 없는 과목만 추가 과목으로 분리한다")
            void it_separates_only_non_standard_subjects() {
                assertThat(result.achievement().newSubjects()).containsExactly("한문");
                assertThat(result.meta().unrecognizedSubjects()).containsExactly("한문");
            }

            @Test
            @DisplayName("성취점수를 표준 과목 순서에 맞추고 없는 과목은 0으로 채운다")
            void it_aligns_scores_with_standard_subject_order() {
                // 순서: 국어, 사회, 도덕, 역사, 수학, 과학, 기술가정, 정보, 영어, 한문
                assertThat(result.achievement().achievement1_2()).containsExactly(5, 4, 0, 0, 5, 0, 4, 5, 0, 4);
                assertThat(result.achievement().achievement2_1()).containsExactly(5, 3, 0, 0, 2, 0, 0, 0, 0, 0);
                assertThat(result.achievement().achievement2_2()).containsExactly(4, 4, 0, 0, 5, 0, 0, 0, 0, 0);
                assertThat(result.achievement().achievement3_1()).containsExactly(5, 4, 0, 0, 3, 0, 0, 0, 0, 0);
            }

            @Test
            @DisplayName("예체능을 학기 우선 순서로 평탄화하고 과목은 체육/음악/미술 순서로 배치한다")
            void it_flattens_arts_physical_by_semester() {
                assertThat(result.achievement().artsPhysicalSubjects()).containsExactly("체육", "음악", "미술");
                // 자유학년제 졸업예정자의 학기: 2-1, 2-2, 3-1
                // 2-1은 체육 A / 음악 B / 미술 C, 2-2는 없음, 3-1은 체육 B
                assertThat(result.achievement().artsPhysicalAchievement()).containsExactly(5, 4, 3, 0, 0, 0, 4, 0, 0);
            }

            @Test
            @DisplayName("자유학년제에서는 1학년 예체능 성적을 배열에 포함하지 않는다")
            void it_excludes_first_grade_arts_physical() {
                // 1학년 2학기에 체육 A가 있지만 자유학년제 학기 목록에 1학년이 없으므로 제외된다
                assertThat(result.achievement().artsPhysicalAchievement()).hasSize(9);
            }

            @Test
            @DisplayName("자유학기제 구분을 응답에 담아 돌려준다")
            void it_echoes_liberal_system() {
                assertThat(result.achievement().liberalSystem()).isEqualTo(MiddleSchoolRecordParser.FREE_YEAR_SYSTEM);
            }

            @Test
            @DisplayName("출결과 봉사시간을 학년별로 집계하고, 읽지 못한 학년은 비워둔다")
            void it_aggregates_attendance_and_volunteer() {
                // 샘플에는 1학년 출결 · 봉사만 있으므로 2 · 3학년은 0이 아니라 null이어야 한다.
                // 0으로 채우면 "개근 · 봉사 0시간"이라는 멀쩡한 값으로 보여서 지원자가 인식 실패를 눈치채지 못한다.
                assertThat(result.achievement().absentDays()).containsExactly(2, null, null);
                assertThat(result.achievement().attendanceDays())
                        .containsExactly(1, 0, 0, null, null, null, null, null, null);
                assertThat(result.achievement().volunteerTime()).containsExactly(7, null, null);
            }

            @Test
            @DisplayName("읽지 못한 학년의 출결과 봉사시간을 검수 대상으로 보고한다")
            void it_reports_missing_attendance_and_volunteer() {
                assertThat(result.meta().warnings())
                        .anyMatch(warning -> warning.type() == ExtractionWarningType.MISSING_ATTENDANCE
                                && "absentDays".equals(warning.field()) && warning.index() == 1)
                        .anyMatch(warning -> warning.type() == ExtractionWarningType.MISSING_VOLUNTEER
                                && "volunteerTime".equals(warning.field()) && warning.index() == 2);
            }

            @Test
            @DisplayName("읽어낸 학년에는 출결 · 봉사 경고를 붙이지 않는다")
            void it_does_not_warn_for_read_attendance() {
                assertThat(result.meta().warnings())
                        .noneMatch(warning -> (warning.type() == ExtractionWarningType.MISSING_ATTENDANCE
                                || warning.type() == ExtractionWarningType.MISSING_VOLUNTEER) && warning.index() == 0);
            }

            @Test
            @DisplayName("학기에서 누락된 과목을 검수 대상으로 보고한다")
            void it_reports_missing_subject() {
                assertThat(result.meta().warnings())
                        .anyMatch(warning -> warning.type() == ExtractionWarningType.MISSING_SUBJECT
                                && "achievement2_1".equals(warning.field()));
            }
        }

        @Nested
        @DisplayName("자유학기제 P 등급과 개근 출결이 섞인 생활기록부가 주어진 경우")
        class Context_with_pass_grade_and_perfect_attendance {

            private static final String TEXT_WITH_PASS_AND_PERFECT = """
                    교과학습발달상황
                    [1학년]
                    학기 교과 과목 원점수/과목평균(표준편차) 성취도(수강자수) 비고
                    1 국어 국어 P
                    1 수학 수학 P
                    1 영어 영어 P
                    출결상황
                    1 190 개근
                    """;

            private ExtractedAchievementResDto result;

            @BeforeEach
            void setUp() {
                result = new MiddleSchoolRecordParser(new SubjectNameNormalizer()).parse(
                        new ExtractedTextDto(TEXT_WITH_PASS_AND_PERFECT, 1, true, ExtractionSource.TEXT_LAYER, 1.0),
                        GraduationType.CANDIDATE,
                        MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM);
            }

            @Test
            @DisplayName("P 등급 과목을 0점으로 채우고 누락 경고를 붙이지 않는다")
            void it_treats_pass_grade_as_not_missing() {
                assertThat(result.achievement().achievement1_1()).containsExactly(0, 0, 0, 0, 0, 0, 0, 0, 0);
                assertThat(result.meta().warnings())
                        .noneMatch(warning -> warning.type() == ExtractionWarningType.MISSING_SUBJECT
                                && "achievement1_1".equals(warning.field())
                                && List.of(0, 4, 8).contains(warning.index()));
            }

            @Test
            @DisplayName("개근 출결을 전부 0으로 채운다")
            void it_treats_perfect_attendance_as_zero() {
                assertThat(result.achievement().absentDays()).containsExactly(0, null, null);
                assertThat(result.achievement().attendanceDays())
                        .containsExactly(0, 0, 0, null, null, null, null, null, null);
                assertThat(result.meta().warnings())
                        .noneMatch(warning -> warning.type() == ExtractionWarningType.MISSING_ATTENDANCE
                                && warning.index() != null && warning.index() == 0);
            }
        }

        @Nested
        @DisplayName("학기 열이 병합 셀이라 첫 과목 행에만 있고 원점수에 쉼표 소수점이 섞인 경우")
        class Context_with_merged_semester_cell_and_comma_decimal {

            private static final String TEXT_WITH_MERGED_SEMESTER = """
                    교과학습발달상황
                    [1학년]
                    학기 교과 과목 원점수/과목평균(표준편차) 성취도(수강자수) 비고
                    1 국어 국어 91/77,8 A(168)
                    사회(역사포함)/도 도덕 88/72.0 B(168) 덕
                    수학 수학 85/70 5 C(168)
                    """;

            private ExtractedAchievementResDto result;

            @BeforeEach
            void setUp() {
                result = new MiddleSchoolRecordParser(new SubjectNameNormalizer()).parse(
                        new ExtractedTextDto(TEXT_WITH_MERGED_SEMESTER, 1, true, ExtractionSource.TEXT_LAYER, 1.0),
                        GraduationType.CANDIDATE,
                        MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM);
            }

            @Test
            @DisplayName("학기 숫자가 없는 뒤 행들도 직전 학기로 채운다")
            void it_carries_over_semester_to_rows_without_leading_digit() {
                // 순서: 국어, 사회, 도덕, 역사, 수학, 과학, 기술가정, 정보, 영어
                assertThat(result.achievement().achievement1_1()).containsExactly(5, 0, 4, 0, 3, 0, 0, 0, 0);
            }
        }

        @Nested
        @DisplayName("학년 · 수업일수와 개근이 서로 다른 줄로 떨어진 출결이 주어진 경우")
        class Context_with_perfect_attendance_split_across_lines {

            private static final String TEXT_WITH_SPLIT_PERFECT_ATTENDANCE = """
                    교과학습발달상황
                    [1학년]
                    학기 교과 과목 원점수/과목평균(표준편차) 성취도(수강자수) 비고
                    1 국어 국어 P
                    출결상황
                    1 190
                    2 191
                    개근
                    개근
                    """;

            private ExtractedAchievementResDto result;

            @BeforeEach
            void setUp() {
                result = new MiddleSchoolRecordParser(new SubjectNameNormalizer())
                        .parse(new ExtractedTextDto(TEXT_WITH_SPLIT_PERFECT_ATTENDANCE,
                                1,
                                true,
                                ExtractionSource.TEXT_LAYER,
                                1.0), GraduationType.CANDIDATE, MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM);
            }

            @Test
            @DisplayName("먼저 나온 학년부터 순서대로 개근을 짝지어 채운다")
            void it_pairs_perfect_attendance_in_order() {
                assertThat(result.achievement().absentDays()).containsExactly(0, 0, null);
                assertThat(result.achievement().attendanceDays()).containsExactly(0, 0, 0, 0, 0, 0, null, null, null);
            }
        }

        @Nested
        @DisplayName("봉사활동이 요약 없이 날짜별 누계시간 기록으로만 주어진 경우")
        class Context_with_volunteer_ledger_only {

            private static final String TEXT_WITH_VOLUNTEER_LEDGER = """
                    창의적 체험활동상황
                    봉사활동실적
                    일자 또는 기간 장소 또는 주관기관명 활동내용 시간 누계시간
                    2021.03.02. 환경정화활동 1 1
                    2021.04.03. 환경정화활동 1 2
                    2022.03.02. 환경정화활동 1 1
                    2022.04.03. 환경정화활동 4 5
                    2023.03.02. 환경정화활동 1 1
                    2023.04.03. 환경정화활동 8 9
                    """;

            private ExtractedAchievementResDto result;

            @BeforeEach
            void setUp() {
                result = new MiddleSchoolRecordParser(new SubjectNameNormalizer()).parse(
                        new ExtractedTextDto(TEXT_WITH_VOLUNTEER_LEDGER, 1, true, ExtractionSource.TEXT_LAYER, 1.0),
                        GraduationType.CANDIDATE,
                        MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM);
            }

            @Test
            @DisplayName("누계시간이 앞줄보다 줄어드는 지점을 학년 경계로 보고 학년별 총 시간을 채운다")
            void it_derives_volunteer_time_from_cumulative_resets() {
                assertThat(result.achievement().volunteerTime()).containsExactly(2, 5, 9);
            }
        }

        @Nested
        @DisplayName("예체능 과목명 행들과 성취도 글자들이 서로 다른 줄 뭉치로 떨어진 경우")
        class Context_with_arts_physical_split_from_achievement {

            private static final String TEXT_WITH_SPLIT_ARTS_PHYSICAL = """
                    교과학습발달상황
                    [1학년]
                    학기 교과 과목 원점수/과목평균(표준편차) 성취도(수강자수) 비고
                    1 국어 국어 P
                    과목 학기 교과 과목
                    1 체육 체육
                    1 예술(음악/미술) 음악
                    2 체육 체육
                    성취도
                    A
                    B
                    C
                    비고
                    """;

            private ExtractedAchievementResDto result;

            @BeforeEach
            void setUp() {
                result = new MiddleSchoolRecordParser(new SubjectNameNormalizer()).parse(
                        new ExtractedTextDto(TEXT_WITH_SPLIT_ARTS_PHYSICAL, 1, true, ExtractionSource.TEXT_LAYER, 1.0),
                        GraduationType.CANDIDATE,
                        MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM);
            }

            @Test
            @DisplayName("과목명이 나온 순서대로 뒤에 몰린 성취도 글자와 짝지어 채운다")
            void it_pairs_arts_physical_subjects_with_achievements_in_order() {
                // 학기: 1-1, 1-2, 2-1, 2-2, 3-1. 1-1(체육 A, 음악 B) → 인덱스 0,1 / 1-2(체육 C) → 인덱스 3
                List<Integer> arts = result.achievement().artsPhysicalAchievement();
                assertThat(arts.get(0)).isEqualTo(5);
                assertThat(arts.get(1)).isEqualTo(4);
                assertThat(arts.get(3)).isEqualTo(3);
            }
        }

        @Nested
        @DisplayName("자유학기제 졸업예정자의 생활기록부가 주어진 경우")
        class Context_with_free_semester_candidate {

            @Test
            @DisplayName("1학년을 포함한 5개 학기 분량의 예체능 배열을 반환한다")
            void it_returns_five_semesters_of_arts_physical() {
                ExtractedAchievementResDto result = parseSample(GraduationType.CANDIDATE,
                        MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM);

                // 학기: 1-1, 1-2, 2-1, 2-2, 3-1 → 5 * 3 = 15
                assertThat(result.achievement().artsPhysicalAchievement()).hasSize(15);
                // 1-2의 체육 A가 두 번째 학기 블록의 첫 칸에 들어간다
                assertThat(result.achievement().artsPhysicalAchievement().get(3)).isEqualTo(5);
            }
        }

        @Nested
        @DisplayName("자유학년제 졸업자의 생활기록부가 주어진 경우")
        class Context_with_free_year_graduate {

            private ExtractedAchievementResDto result;

            @BeforeEach
            void setUp() {
                result = parseSample(GraduationType.GRADUATE, null);
            }

            @Test
            @DisplayName("4개 학기 분량의 예체능 배열을 반환한다")
            void it_returns_four_semesters_of_arts_physical() {
                // 학기: 2-1, 2-2, 3-1, 3-2 → 4 * 3 = 12
                assertThat(result.achievement().artsPhysicalAchievement()).hasSize(12);
            }

            @Test
            @DisplayName("찾지 못한 3학년 2학기를 누락 학기로 보고한다")
            void it_reports_missing_semester() {
                assertThat(result.meta().missingSemesters()).containsExactly("3-2");
                assertThat(result.meta().warnings())
                        .anyMatch(warning -> warning.type() == ExtractionWarningType.MISSING_SEMESTER);
            }
        }

        @Nested
        @DisplayName("자유학기제 졸업자의 생활기록부가 주어진 경우")
        class Context_with_free_semester_graduate {

            private ExtractedAchievementResDto result;

            @BeforeEach
            void setUp() {
                result = parseSample(GraduationType.GRADUATE, MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM);
            }

            @Test
            @DisplayName("1학년을 포함한 6개 학기 분량의 예체능 배열을 반환한다")
            void it_returns_six_semesters_of_arts_physical() {
                // 학기: 1-1, 1-2, 2-1, 2-2, 3-1, 3-2 → 6 * 3 = 18
                assertThat(result.achievement().artsPhysicalAchievement()).hasSize(18);
                // 1-2의 체육 A가 두 번째 학기 블록의 첫 칸에 들어간다
                assertThat(result.achievement().artsPhysicalAchievement().get(3)).isEqualTo(5);
                // 예체능 성적을 찾지 못한 과목은 0으로 채워진다 (1-1·2-2·3-2 전체 누락 9칸 + 1-2·3-1의 음악·미술 누락 4칸)
                assertThat(result.achievement().artsPhysicalAchievement().stream().filter(score -> score == 0))
                        .hasSize(13);
            }

            @Test
            @DisplayName("찾지 못한 1학년 1학기와 3학년 2학기를 누락 학기로 보고한다")
            void it_reports_missing_semesters() {
                assertThat(result.meta().missingSemesters()).containsExactly("1-1", "3-2");
                assertThat(result.meta().warnings())
                        .anyMatch(warning -> warning.type() == ExtractionWarningType.MISSING_SEMESTER);
            }
        }

        @Nested
        @DisplayName("성적 표를 찾을 수 없는 텍스트가 주어진 경우")
        class Context_with_unparseable_text {

            @Test
            @DisplayName("예외를 던지지 않고 빈 결과를 반환한다")
            void it_returns_empty_result_without_throwing() {
                ExtractedAchievementResDto result = parser.parse(
                        new ExtractedTextDto("의미 없는 텍스트입니다.", 1, true, ExtractionSource.TEXT_LAYER, 1.0),
                        GraduationType.CANDIDATE,
                        MiddleSchoolRecordParser.FREE_YEAR_SYSTEM);

                assertThat(result.achievement().newSubjects()).isEmpty();
                assertThat(result.achievement().achievement2_1()).isNull();
                assertThat(result.meta().missingSemesters()).containsExactly("2-1", "2-2", "3-1");
            }
        }
    }

    @Nested
    @DisplayName("과목명 정규화는")
    class Describe_normalize {

        @Test
        @DisplayName("표기 장식이 붙은 과목명을 표준 과목명으로 변환한다")
        void it_normalizes_decorated_subject_names() {
            SubjectNameNormalizer normalizer = new SubjectNameNormalizer();

            assertThat(normalizer.normalize("기술·가정").normalized()).isEqualTo("기술가정");
            assertThat(normalizer.normalize("사회(역사포함)").normalized()).isEqualTo("사회");
            assertThat(normalizer.normalize("정보").matchType()).isEqualTo(SubjectNameNormalizer.MatchType.EXACT);
            assertThat(normalizer.normalize("한문").matchType()).isEqualTo(SubjectNameNormalizer.MatchType.UNKNOWN);
        }

        @Test
        @DisplayName("예체능 과목을 구분한다")
        void it_identifies_arts_physical_subjects() {
            SubjectNameNormalizer normalizer = new SubjectNameNormalizer();

            assertThat(List.of("체육", "음악", "미술")).allMatch(normalizer::isArtsPhysical);
            assertThat(normalizer.isArtsPhysical("국어")).isFalse();
        }
    }
}
