package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("kordoc 과목명 토큰 분리 테스트")
class KordocSubjectTokenizerTest {

    private KordocSubjectTokenizer tokenizer;

    @BeforeEach
    void setUp() {
        tokenizer = new KordocSubjectTokenizer();
    }

    @Nested
    @DisplayName("tokenize 메서드는")
    class Describe_tokenize {

        @Nested
        @DisplayName("표준 과목 9개로만 이루어진 문자열이 주어진 경우")
        class Context_with_all_standard_subjects {

            @Test
            @DisplayName("순서대로 분리한다")
            void it_splits_in_order() {
                Optional<List<String>> result = tokenizer.tokenize("국어사회도덕역사수학과학기술가정정보영어", 9);

                assertThat(result).isPresent();
                assertThat(result.get()).containsExactly("국어", "사회", "도덕", "역사", "수학", "과학", "기술가정", "정보", "영어");
            }
        }

        @Nested
        @DisplayName("표준 과목 일부만 있는 경우")
        class Context_with_subset_of_standard_subjects {

            @Test
            @DisplayName("있는 만큼만 순서대로 분리한다")
            void it_splits_subset() {
                Optional<List<String>> result = tokenizer.tokenize("사회수학영어", 3);

                assertThat(result).isPresent();
                assertThat(result.get()).containsExactly("사회", "수학", "영어");
            }
        }

        @Nested
        @DisplayName("사전에 없는 과목명(선택 과목 등)이 섞여 있는 경우")
        class Context_with_unknown_subject {

            @Test
            @DisplayName("빈 값을 반환한다")
            void it_returns_empty() {
                Optional<List<String>> result = tokenizer.tokenize("국어한문수학", 3);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("분리된 개수가 기대 개수와 다른 경우")
        class Context_with_count_mismatch {

            @Test
            @DisplayName("빈 값을 반환한다")
            void it_returns_empty() {
                Optional<List<String>> result = tokenizer.tokenize("국어사회도덕", 2);

                assertThat(result).isEmpty();
            }
        }

        @Nested
        @DisplayName("빈 문자열이 주어진 경우")
        class Context_with_blank_input {

            @Test
            @DisplayName("빈 값을 반환한다")
            void it_returns_empty() {
                assertThat(tokenizer.tokenize("   ", 1)).isEmpty();
            }
        }
    }
}
