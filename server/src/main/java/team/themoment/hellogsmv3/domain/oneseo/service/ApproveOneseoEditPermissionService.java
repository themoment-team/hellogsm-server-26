package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class ApproveOneseoEditPermissionService {

    private final OneseoService oneseoService;

    @Transactional
    public void execute(Long memberId) {
        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        if (oneseo.getOneseoEditStatus() != OneseoEditStatus.REQUESTED) {
            throw new ExpectedException("원서 수정 권한 요청이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        oneseo.approveEditPermit();
    }
}
