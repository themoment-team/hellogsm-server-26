package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

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
import team.themoment.hellogsmv3.domain.oneseo.dto.request.InterviewScoreReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("ModifyInterviewScoreService 클래스의")
public class ModifyInterviewScoreServiceTest {

    @Mock
    private MemberService memberService;
    @Mock
    private OneseoService oneseoService;
    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;

    @InjectMocks
    private ModifyInterviewScoreService modifyInterviewScoreService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private final BigDecimal updatedInterviewScore = BigDecimal.valueOf(99.25);

        @Nested
        @DisplayName("존재하는 회원 ID와 심층면접 점수가 주어지면")
        class Context_with_existing_member_id_and_interview_score {

            EntranceTestResult entranceTestResult;

            @BeforeEach
            void setUp() {
                Member member = Member.builder().id(memberId).build();

                entranceTestResult = EntranceTestResult.builder().interviewScore(BigDecimal.valueOf(75))
                        .firstTestPassYn(null).build();

                Oneseo oneseo = Oneseo.builder().member(member).entranceTestResult(entranceTestResult).build();

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
            }

            @Test
            @DisplayName("심층면접 점수를 저장한다.")
            void it_saves_interview_score() {
                InterviewScoreReqDto interviewScoreReqDto = new InterviewScoreReqDto(updatedInterviewScore);
                modifyInterviewScoreService.execute(memberId, interviewScoreReqDto);

                ArgumentCaptor<EntranceTestResult> entranceTestResultCaptor = ArgumentCaptor
                        .forClass(EntranceTestResult.class);

                verify(entranceTestResultRepository).save(entranceTestResultCaptor.capture());

                EntranceTestResult capturedEntranceTestResult = entranceTestResultCaptor.getValue();

                assertEquals(updatedInterviewScore, capturedEntranceTestResult.getInterviewScore());
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
                InterviewScoreReqDto interviewScoreReqDto = new InterviewScoreReqDto(updatedInterviewScore);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyInterviewScoreService.execute(memberId, interviewScoreReqDto));

                assertEquals("존재하지 않는 지원자입니다. member ID: ", exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("원서가 존재하지 않는다면")
        class Context_with_non_existing_oneseo {

            @BeforeEach
            void setUp() {
                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willThrow(
                        new ExpectedException("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                InterviewScoreReqDto interviewScoreReqDto = new InterviewScoreReqDto(updatedInterviewScore);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyInterviewScoreService.execute(memberId, interviewScoreReqDto));

                assertEquals("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
