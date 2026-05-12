package team.themoment.hellogsmv3.domain.member.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import team.themoment.hellogsmv3.domain.member.dto.response.FoundMemberFirstTestResDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;

@ExtendWith(MockitoExtension.class)
@DisplayName("QueryMyFirstTestResultService 클래스의")
class QueryMyFirstTestResultServiceTest {
    @Mock
    private OneseoService oneseoService;

    @InjectMocks
    private QueryMyFirstTestResultService queryMyFirstTestResultService;

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private Oneseo oneseo;

        @BeforeEach
        void setUp() {
            Member member = Member.builder().id(memberId).build();

            oneseo = Oneseo.builder().member(member)
                    .entranceTestResult(EntranceTestResult.builder().firstTestPassYn(YesNo.YES).build()).build();

            given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
        }

        @Nested
        @DisplayName("1차 테스트 결과가 발표된 경우")
        class Context_with_first_test_result_announced {

            @BeforeEach
            void setUp() {
                given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(false);
            }

            @Test
            @DisplayName("1차 테스트 결과를 반환한다")
            void it_returns_first_test_result() {
                FoundMemberFirstTestResDto result = queryMyFirstTestResultService.execute(memberId);
                assertEquals(oneseo.getEntranceTestResult().getFirstTestPassYn(), result.firstTestPassYn());
            }
        }

        @Nested
        @DisplayName("1차 테스트 결과가 발표 되지 않은 경우")
        class Context_with_first_test_result_not_announced {

            @BeforeEach
            void setUp() {
                given(oneseoService.validateFirstTestResultAnnouncement()).willReturn(true);
            }

            @Test
            @DisplayName("null을 반환한다")
            void it_returns_null() {
                FoundMemberFirstTestResDto result = queryMyFirstTestResultService.execute(memberId);
                assertNull(result);
            }
        }
    }
}
