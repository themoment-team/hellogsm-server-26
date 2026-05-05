package team.themoment.hellogsmv3.domain.member.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import team.themoment.hellogsmv3.domain.member.dto.response.FoundMemberSecondTestResDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;

@DisplayName("QueryMySecondTestResultService 클래스의")
class QueryMySecondTestResultServiceTest {

    @Mock
    private OneseoService oneseoService;

    @InjectMocks
    private QueryMySecondTestResultService queryMySecondTestResultService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private Oneseo oneseo;

        @BeforeEach
        void setUp() {
            Member member = Member.builder().id(memberId).build();

            oneseo = Oneseo.builder().member(member)
                    .entranceTestResult(EntranceTestResult.builder().secondTestPassYn(YesNo.YES).build())
                    .decidedMajor(Major.SW).build();

            given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
        }

        @Nested
        @DisplayName("2차 테스트 결과가 발표된 경우")
        class Context_with_second_test_result_announced {

            @BeforeEach
            void setUp() {
                given(oneseoService.validateSecondTestResultAnnouncement()).willReturn(false);
            }

            @Test
            @DisplayName("2차 테스트 결과와 배정된 학과를 반환한다")
            void it_returns_second_test_result_and_decided_major() {
                FoundMemberSecondTestResDto result = queryMySecondTestResultService.execute(memberId);
                assertEquals(oneseo.getEntranceTestResult().getSecondTestPassYn(), result.secondTestPassYn());
                assertEquals(oneseo.getDecidedMajor(), result.decidedMajor());
            }
        }

        @Nested
        @DisplayName("2차 테스트 결과가 발표 되지 않은 경우")
        class Context_with_second_test_result_not_announced {

            @BeforeEach
            void setUp() {
                given(oneseoService.validateSecondTestResultAnnouncement()).willReturn(true);
            }

            @Test
            @DisplayName("null을 반환한다")
            void it_returns_null() {
                FoundMemberSecondTestResDto result = queryMySecondTestResultService.execute(memberId);
                assertNull(result);
            }
        }
    }
}
