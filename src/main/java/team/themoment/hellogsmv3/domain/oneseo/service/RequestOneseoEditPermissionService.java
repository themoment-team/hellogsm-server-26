package team.themoment.hellogsmv3.domain.oneseo.service;

import java.time.LocalTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class RequestOneseoEditPermissionService {

    private static final LocalTime REQUEST_START = LocalTime.of(9, 0);
    private static final LocalTime REQUEST_END = LocalTime.of(16, 0);

    private final OneseoService oneseoService;

    @Transactional
    public void execute(Long memberId) {
        LocalTime now = LocalTime.now();
        if (now.isBefore(REQUEST_START) || now.isAfter(REQUEST_END)) {
            throw new ExpectedException("원서 수정 권한 요청은 09:00 ~ 16:00 사이에만 가능합니다.", HttpStatus.FORBIDDEN);
        }

        Oneseo oneseo = oneseoService.findWithMemberByMemberIdOrThrow(memberId);

        OneseoService.isBeforeFirstTest(oneseo.getEntranceTestResult().getFirstTestPassYn());

        if (oneseo.getOneseoEditStatus() == OneseoEditStatus.APPROVED) {
            throw new ExpectedException("이미 원서 수정 권한이 부여된 상태입니다.", HttpStatus.CONFLICT);
        }
        if (oneseo.getOneseoEditStatus() == OneseoEditStatus.REQUESTED) {
            throw new ExpectedException("이미 원서 수정 권한 요청이 진행 중입니다.", HttpStatus.CONFLICT);
        }

        oneseo.requestEditPermit();
    }
}
