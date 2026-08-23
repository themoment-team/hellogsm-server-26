package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocParseResult;
import team.themoment.sdk.exception.ExpectedException;
import tools.jackson.databind.ObjectMapper;

@DisplayName("kordoc CLI OCR 클라이언트 테스트")
class KordocCliOcrClientTest {

    private KordocCliOcrClient client;

    @BeforeEach
    void setUp() {
        client = new KordocCliOcrClient(new ObjectMapper());
    }

    @Nested
    @DisplayName("parseJson 메서드는")
    class Describe_parseJson {

        @Nested
        @DisplayName("kordoc이 출력하는 형식의 JSON이 주어진 경우")
        class Context_with_valid_kordoc_json {

            private static final String JSON = """
                    {
                      "blocks": [
                        {
                          "type": "heading",
                          "text": "1학년",
                          "pageNumber": 1
                        },
                        {
                          "type": "table",
                          "pageNumber": 1,
                          "table": {
                            "cells": [
                              [
                                {"text": "국어", "colSpan": 1, "rowSpan": 1},
                                {"text": "A", "colSpan": 1, "rowSpan": 1}
                              ]
                            ]
                          }
                        }
                      ]
                    }
                    """;

            @Test
            @DisplayName("블록과 표를 KordocParseResult로 역직렬화한다")
            void it_deserializes_into_kordoc_parse_result() {
                KordocParseResult result = client.parseJson(JSON);

                assertThat(result.blocksOrEmpty()).hasSize(2);
                assertThat(result.blocksOrEmpty().get(0).textOrEmpty()).isEqualTo("1학년");
                assertThat(result.blocksOrEmpty().get(1).isTable()).isTrue();
                assertThat(result.blocksOrEmpty().get(1).tableRowsOrEmpty().get(0).get(0).textOrEmpty())
                        .isEqualTo("국어");
            }
        }

        @Nested
        @DisplayName("알 수 없는 필드가 섞인 JSON이 주어진 경우")
        class Context_with_unknown_fields {

            @Test
            @DisplayName("알 수 없는 필드는 무시하고 역직렬화한다")
            void it_ignores_unknown_fields() {
                String json = """
                        {
                          "blocks": [
                            {"type": "text", "text": "안내문", "pageNumber": 1, "confidence": 0.9}
                          ],
                          "meta": {"engine": "kordoc"}
                        }
                        """;

                KordocParseResult result = client.parseJson(json);

                assertThat(result.blocksOrEmpty()).hasSize(1);
                assertThat(result.blocksOrEmpty().get(0).textOrEmpty()).isEqualTo("안내문");
            }
        }

        @Nested
        @DisplayName("깨진 JSON이 주어진 경우")
        class Context_with_malformed_json {

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                assertThatThrownBy(() -> client.parseJson("{ not-json")).isInstanceOf(ExpectedException.class);
            }
        }
    }
}
