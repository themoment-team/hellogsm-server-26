package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.FoundMemberAndOneseoDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OneseoEditabilityResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

@DisplayName("QueryOneseoEditabilityService 클래스의")
class QueryOneseoEditabilityServiceTest {

    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;

    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private QueryOneseoEditabilityService queryOneseoEditabilityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        @Nested
        @DisplayName("1차 전형이 종료되지 않은 경우")
        class Context_when_first_test_not_finished {

            @BeforeEach
            void setUp() {
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNotNull()).willReturn(false);
            }

            @Nested
            @DisplayName("원서가 존재하는 경우")
            class Context_with_existing_oneseo {

                @BeforeEach
                void setUp() {
                    Oneseo oneseo = mock(Oneseo.class);
                    given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.NONE);
                    given(oneseoRepository.findMemberAndOneseoByMemberId(1L))
                            .willReturn(new FoundMemberAndOneseoDto(null, oneseo));
                }

                @Test
                @DisplayName("수정 가능 여부가 true이고 oneseoEditStatus를 반환한다")
                void it_returns_true_editability_with_edit_status() {
                    OneseoEditabilityResDto result = queryOneseoEditabilityService.execute(1L);
                    assertEquals(true, result.oneseoEditability());
                    assertEquals(OneseoEditStatus.NONE, result.oneseoEditStatus());
                }
            }

            @Nested
            @DisplayName("원서가 존재하지 않는 경우")
            class Context_without_oneseo {

                @BeforeEach
                void setUp() {
                    given(oneseoRepository.findMemberAndOneseoByMemberId(1L))
                            .willReturn(new FoundMemberAndOneseoDto(null, null));
                }

                @Test
                @DisplayName("수정 가능 여부가 true이고 oneseoEditStatus는 null을 반환한다")
                void it_returns_true_editability_with_null_status() {
                    OneseoEditabilityResDto result = queryOneseoEditabilityService.execute(1L);
                    assertEquals(true, result.oneseoEditability());
                    assertNull(result.oneseoEditStatus());
                }
            }
        }

        @Nested
        @DisplayName("1차 전형이 종료된 경우")
        class Context_when_first_test_finished {

            @BeforeEach
            void setUp() {
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNotNull()).willReturn(true);
                Oneseo oneseo = mock(Oneseo.class);
                given(oneseo.getOneseoEditStatus()).willReturn(OneseoEditStatus.NONE);
                given(oneseoRepository.findMemberAndOneseoByMemberId(1L))
                        .willReturn(new FoundMemberAndOneseoDto(null, oneseo));
            }

            @Test
            @DisplayName("수정 가능 여부가 false인 OneseoEditabilityResDto를 반환한다")
            void it_returns_false_editability() {
                OneseoEditabilityResDto result = queryOneseoEditabilityService.execute(1L);
                assertEquals(false, result.oneseoEditability());
            }
        }
    }
}
