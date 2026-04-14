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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.CompetencyEvaluationScoreReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("ModifyCompetencyEvaluationScoreService 클래스의")
public class ModifyCompetencyEvaluationScoreServiceTest {

    @Mock
    private MemberService memberService;
    @Mock
    private OneseoService oneseoService;
    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;

    @InjectMocks
    private ModifyCompetencyEvaluationScoreService modifyCompetencyEvaluationScoreService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {
        private final Long memberId = 1L;
        private final BigDecimal newScore = BigDecimal.valueOf(85);

        @Nested
        @DisplayName("존재하는 회원 ID와 역량검사 점수가 주어지면")
        class Context_with_existing_member_id_and_competency_evaluation_score {
            EntranceTestResult entranceTestResult;

            @BeforeEach
            void setUp() {
                Member member = Member.builder().id(memberId).build();

                entranceTestResult = EntranceTestResult.builder().competencyEvaluationScore(BigDecimal.valueOf(70))
                        .secondTestPassYn(null).build();

                Oneseo oneseo = Oneseo.builder().member(member).entranceTestResult(entranceTestResult).build();

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
            }

            @Test
            @DisplayName("역량검사 점수를 저장한다.")
            void it_save_competency_evaluation_score() {
                CompetencyEvaluationScoreReqDto competencyEvaluationScoreReqDto = new CompetencyEvaluationScoreReqDto(
                        newScore);

                modifyCompetencyEvaluationScoreService.execute(memberId, competencyEvaluationScoreReqDto);

                assertEquals(newScore, entranceTestResult.getCompetencyEvaluationScore());
                verify(entranceTestResultRepository).save(entranceTestResult);
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
                CompetencyEvaluationScoreReqDto competencyEvaluationScoreReqDto = new CompetencyEvaluationScoreReqDto(
                        newScore);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyCompetencyEvaluationScoreService.execute(memberId,
                                competencyEvaluationScoreReqDto));

                assertEquals("존재하지 않는 지원자입니다. member ID: ", exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
