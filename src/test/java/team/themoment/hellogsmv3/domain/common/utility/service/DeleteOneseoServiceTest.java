package team.themoment.hellogsmv3.domain.common.utility.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("DeleteOneseoService 클래스의")
class DeleteOneseoServiceTest {

    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private DeleteOneseoService deleteOneseoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final String submitCode = "A-1";
        private final Long memberId = 1L;

        @Nested
        @DisplayName("유효한 접수 번호가 주어지면")
        class Context_with_valid_submit_code {

            @BeforeEach
            void setUp() {
                given(oneseoRepository.findMemberIdByOneseoSubmitCode(submitCode)).willReturn(Optional.of(memberId));
            }

            @Test
            @DisplayName("원서를 삭제하고 memberId를 반환한다")
            void it_deletes_oneseo_and_returns_member_id() {
                Long result = deleteOneseoService.execute(submitCode);

                verify(oneseoRepository).deleteByOneseoSubmitCode(submitCode);
                assertEquals(memberId, result);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 접수 번호가 주어지면")
        class Context_with_non_existing_submit_code {

            @BeforeEach
            void setUp() {
                given(oneseoRepository.findMemberIdByOneseoSubmitCode(submitCode)).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> deleteOneseoService.execute(submitCode));

                assertEquals("해당 접수 번호에 해당하는 원서가 존재하지 않습니다.", exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
