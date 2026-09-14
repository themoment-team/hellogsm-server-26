package team.themoment.hellogsmv3.domain.oneseo.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.CompetencyEvaluationScoreReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;

@Service
@RequiredArgsConstructor
public class ModifyCompetencyEvaluationScoreService {

    private final MemberService memberService;
    private final OneseoService oneseoService;
    private final EntranceTestResultRepository entranceTestResultRepository;

    @Transactional
    public void execute(Long memberId, CompetencyEvaluationScoreReqDto competencyEvaluationScoreReqDto) {
        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
        OneseoService.isBeforeSecondTest(entranceTestResult.getSecondTestPassYn());

        BigDecimal competencyEvaluationScore = competencyEvaluationScoreReqDto.competencyEvaluationScore();
        entranceTestResult.modifyCompetencyEvaluationScore(competencyEvaluationScore);

        entranceTestResultRepository.save(entranceTestResult);
    }
}
