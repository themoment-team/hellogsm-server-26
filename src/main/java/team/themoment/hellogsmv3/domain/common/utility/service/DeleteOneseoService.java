package team.themoment.hellogsmv3.domain.common.utility.service;

import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.ONESEO_CACHE_VALUE;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
@Profile("!prod")
public class DeleteOneseoService {

    private final OneseoRepository oneseoRepository;

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = ONESEO_CACHE_VALUE, key = "#result")
    public Long execute(String submitCode) {
        Long memberId = oneseoRepository.findMemberIdByOneseoSubmitCode(submitCode)
                .orElseThrow(() -> new ExpectedException("해당 접수 번호에 해당하는 원서가 존재하지 않습니다.", HttpStatus.NOT_FOUND));
        oneseoRepository.deleteByOneseoSubmitCode(submitCode);
        return memberId;
    }
}
