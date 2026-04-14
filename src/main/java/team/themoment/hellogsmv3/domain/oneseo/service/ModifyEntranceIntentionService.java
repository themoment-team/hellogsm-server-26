package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.EntranceIntentionReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class ModifyEntranceIntentionService {

    private final OneseoRepository oneseoRepository;
    private final MemberService memberService;
    private final OneseoService oneseoService;

    @Transactional
    public void execute(Long memberId, EntranceIntentionReqDto reqDto) {
        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        isAfterFinalTest(oneseo);

        oneseo.modifyEntranceIntentionYn(reqDto.entranceIntentionYn());

        oneseoRepository.save(oneseo);
    }

    private void isAfterFinalTest(Oneseo oneseo) {
        if (oneseo.getDecidedMajor() == null) {
            throw new ExpectedException("최종 합격한 지원자의 원서만 입학등록 동의서 제출여부를 수정할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }
    }
}
