package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.FoundMemberAndOneseoDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OneseoEditabilityResDto;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

@Service
@RequiredArgsConstructor
public class QueryOneseoEditabilityService {

    private final EntranceTestResultRepository entranceTestResultRepository;
    private final OneseoRepository oneseoRepository;

    @Transactional(readOnly = true)
    public OneseoEditabilityResDto execute(Long memberId) {
        boolean isFirstTestFinished = entranceTestResultRepository.existsByFirstTestPassYnIsNotNull();
        FoundMemberAndOneseoDto queryResult = oneseoRepository.findMemberAndOneseoByMemberId(memberId);
        if (queryResult.oneseo() == null) {
            return new OneseoEditabilityResDto(!isFirstTestFinished, null);
        }
        return new OneseoEditabilityResDto(!isFirstTestFinished, queryResult.oneseo().getOneseoEditStatus());
    }
}
