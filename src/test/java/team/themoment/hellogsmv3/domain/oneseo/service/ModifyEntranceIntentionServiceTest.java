package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.EntranceIntentionReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("ModifyEntranceIntentionService 클래스의")
public class ModifyEntranceIntentionServiceTest {

    @Mock
    private MemberService memberService;
    @Mock
    private OneseoService oneseoService;
    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private ModifyEntranceIntentionService modifyEntranceIntentionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private final YesNo defaultYn = YesNo.YES;
        private final YesNo targetYn = YesNo.NO;

        @Nested
        @DisplayName("존재하는 회원 ID가 주어지면")
        class Context_with_existing_member_id {

            Member member;

            @BeforeEach
            void setUp() {
                member = Member.builder().id(memberId).build();
            }

            @Nested
            @DisplayName("원서가 있다면")
            class Context_with_existing_oneseo {

                @Nested
                @DisplayName("최종합격된 원서라면")
                class Context_with_final_oneseo {

                    Oneseo oneseo;

                    @BeforeEach
                    void setUp() {
                        oneseo = Oneseo.builder().member(member).decidedMajor(Major.SW).entranceIntentionYn(defaultYn)
                                .build();

                        given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                    }

                    @Test
                    @DisplayName("입학 의사 여부를 수정하고 저장한다.")
                    void it_saves_entrance_intention() {
                        EntranceIntentionReqDto reqDto = new EntranceIntentionReqDto(targetYn);
                        modifyEntranceIntentionService.execute(memberId, reqDto);

                        ArgumentCaptor<Oneseo> oneseoCaptor = ArgumentCaptor.forClass(Oneseo.class);

                        verify(oneseoRepository).save(oneseoCaptor.capture());

                        Oneseo capturedOneseo = oneseoCaptor.getValue();

                        assertEquals(targetYn, capturedOneseo.getEntranceIntentionYn());
                    }
                }

                @Nested
                @DisplayName("최종합격된 원서가 아니라면")
                class Context_with_non_final_oneseo {

                    @BeforeEach
                    void setUp() {
                        Oneseo oneseo = Oneseo.builder().member(member).decidedMajor(null) // 최종 합격하지 않은 상태
                                .build();

                        given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                    }

                    @Test
                    @DisplayName("ExpectedException을 던진다")
                    void it_throws_expected_exception() {
                        EntranceIntentionReqDto reqDto = new EntranceIntentionReqDto(defaultYn);

                        ExpectedException exception = assertThrows(ExpectedException.class,
                                () -> modifyEntranceIntentionService.execute(memberId, reqDto));

                        assertEquals("최종 합격한 지원자의 원서만 입학등록 동의서 제출여부를 수정할 수 있습니다.", exception.getMessage());
                        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
                    }
                }
            }

            @Nested
            @DisplayName("원서가 없다면")
            class Context_with_non_existing_oneseo {

                @BeforeEach
                void setUp() {
                    given(oneseoService.findWithMemberByMemberIdOrThrow(memberId))
                            .willThrow(new ExpectedException("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId,
                                    HttpStatus.NOT_FOUND));
                }

                @Test
                @DisplayName("ExpectedException을 던진다")
                void it_throws_expected_exception() {
                    EntranceIntentionReqDto reqDto = new EntranceIntentionReqDto(defaultYn);

                    ExpectedException exception = assertThrows(ExpectedException.class,
                            () -> modifyEntranceIntentionService.execute(memberId, reqDto));

                    assertEquals("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, exception.getMessage());
                    assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
                }
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member_id {

            @BeforeEach
            void setUp() {
                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willThrow(
                        new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                EntranceIntentionReqDto reqDto = new EntranceIntentionReqDto(defaultYn);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyEntranceIntentionService.execute(memberId, reqDto));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
