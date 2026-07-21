package team.themoment.hellogsmv3.domain.oneseo.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.InterviewScoreReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;

@Service
@RequiredArgsConstructor
public class ModifyInterviewScoreService {

    private final MemberService memberService;
    private final OneseoService oneseoService;
    private final EntranceTestResultRepository entranceTestResultRepository;

    @Transactional
    public void execute(Long memberId, InterviewScoreReqDto reqDto) {
        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
        OneseoService.isBeforeSecondTest(entranceTestResult.getSecondTestPassYn());

        BigDecimal interviewScore = reqDto.interviewScore();
        entranceTestResult.modifyInterviewScore(interviewScore);

        entranceTestResultRepository.save(entranceTestResult);
    }
}
