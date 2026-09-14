package team.themoment.hellogsmv3.domain.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.dto.response.FoundMemberFirstTestResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.service.OneseoService;

@Service
@RequiredArgsConstructor
public class QueryMyFirstTestResultService {

    private final MemberService memberService;
    private final OneseoService oneseoService;

    @Transactional(readOnly = true)
    public FoundMemberFirstTestResDto execute(Long memberId) {
        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        // no content response status
        if (oneseoService.validateFirstTestResultAnnouncement())
            return null;

        EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
        return new FoundMemberFirstTestResDto(entranceTestResult.getFirstTestPassYn());
    }
}
