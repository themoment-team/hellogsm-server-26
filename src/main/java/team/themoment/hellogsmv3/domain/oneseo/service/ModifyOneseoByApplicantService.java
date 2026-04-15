package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class ModifyOneseoByApplicantService {

    private final OneseoService oneseoService;
    private final ModifyOneseoService modifyOneseoService;

    @Transactional
    public void execute(OneseoReqDto reqDto, Long memberId) {
        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        if (oneseo.getOneseoEditStatus() != OneseoEditStatus.APPROVED) {
            throw new ExpectedException("원서 수정 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        modifyOneseoService.execute(reqDto, memberId);
    }
}
