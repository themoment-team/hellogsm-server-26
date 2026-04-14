package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ArrivedStatusResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("ModifyRealOneseoArrivedYnService 클래스의")
public class ModifyRealOneseoArrivedYnServiceTest {

    @Mock
    private MemberService memberService;

    @Mock
    private OneseoService oneseoService;

    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private ModifyRealOneseoArrivedYnService modifyRealOneseoArrivedYnService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {
        private final Long memberId = 1L;

        @Nested
        @DisplayName("존재하는 회원 ID가 주어지면")
        class Context_with_existing_member_id {
            Oneseo oneseo;

            @BeforeEach
            void setUp() {
                Member member = Member.builder().id(memberId).build();

                EntranceTestResult entranceTestResult = EntranceTestResult.builder().firstTestPassYn(null).build();

                oneseo = Oneseo.builder().member(member).realOneseoArrivedYn(YesNo.NO)
                        .wantedScreening(Screening.GENERAL).entranceTestResult(entranceTestResult).build();

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                given(oneseoRepository.save(oneseo)).willReturn(oneseo);
            }

            @Test
            @DisplayName("원서 도착 여부를 전환하고, 해당 정보를 반환한다.")
            void it_switch_and_returns_arrived_status() {
                ArrivedStatusResDto result = modifyRealOneseoArrivedYnService.execute(memberId);

                assertNotNull(result);
                assertEquals(YesNo.YES, result.realOneseoArrivedYn());

                verify(oneseoRepository, times(1)).save(oneseo);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member_id {

            @BeforeEach
            void setUp() {
                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId))
                        .willThrow(new ExpectedException("존재하지 않는 지원자입니다. member ID: ", HttpStatus.NOT_FOUND));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyRealOneseoArrivedYnService.execute(memberId));

                assertEquals("존재하지 않는 지원자입니다. member ID: ", exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
