package team.themoment.hellogsmv3.domain.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;
import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.dto.request.CreateMemberReqDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;

@DisplayName("CreateMemberService 클래스의")
class CreateMemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private MemberService memberService;
    @Mock
    private ScheduleEnvironment scheduleEnvironment;
    @Mock
    private OneseoRepository oneseoRepository;
    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;
    @Mock
    private CommonCodeService commonCodeService;
    @InjectMocks
    private CreateMemberService createMemberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private final CreateMemberReqDto reqDto = new CreateMemberReqDto("validCode",
                "최장우",
                "01012345678",
                Sex.MALE,
                LocalDate.of(2006, 3, 6));

        @Nested
        @DisplayName("유효한 회원 ID와 요청 데이터가 주어지면")
        class Context_with_valid_member_id_and_request_data {

            private Member existingMember;

            @BeforeEach
            void setUp() {
                existingMember = Member.builder().id(memberId).email("jangwooooo@example.com")
                        .authReferrerType(AuthReferrerType.GOOGLE).role(Role.UNAUTHENTICATED).build();

                given(memberService.findByIdOrThrow(memberId)).willReturn(existingMember);
                given(scheduleEnvironment.oneseoSubmissionEnd())
                        .willReturn(LocalDateTime.of(9999, Month.OCTOBER, 10, 10, 10));
                given(memberRepository.findByPhoneNumber(reqDto.phoneNumber())).willReturn(Optional.empty());
                given(entranceTestResultRepository.existsByFirstTestPassYnIsNotNull()).willReturn(false);
                willDoNothing().given(commonCodeService)
                        .validateAndDelete(memberId, reqDto.code(), reqDto.phoneNumber(), SIGNUP);
            }

            @Test
            @DisplayName("새로운 회원을 생성하고 저장 후 Role을 반환한다")
            void it_creates_and_saves_new_member() {
                Role result = createMemberService.execute(reqDto, memberId);

                verify(memberRepository).save(
                        org.mockito.ArgumentMatchers.argThat(member -> member.getId().equals(existingMember.getId())
                                && member.getEmail().equals(existingMember.getEmail())
                                && member.getAuthReferrerType().equals(existingMember.getAuthReferrerType())
                                && member.getName().equals(reqDto.name()) && member.getBirth().equals(reqDto.birth())
                                && member.getPhoneNumber().equals(reqDto.phoneNumber())
                                && member.getSex().equals(reqDto.sex()) && member.getRole().equals(Role.APPLICANT)));
                assertEquals(Role.APPLICANT, result);
            }
        }

        @Nested
        @DisplayName("회원 ID가 유효하지 않으면")
        class Context_with_invalid_member_id {

        @BeforeEach
      void setUp() {
        when(memberService.findByIdOrThrow(memberId))
            .thenThrow(
                new ExpectedException(
                    "존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
        willDoNothing()
            .given(commonCodeService)
            .validateAndDelete(memberId, reqDto.code(), reqDto.phoneNumber(), SIGNUP);
      }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> createMemberService.execute(reqDto, memberId));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
