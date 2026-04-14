package team.themoment.hellogsmv3.domain.common.operation.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.operation.dto.response.AnnounceTestResultResDto;
import team.themoment.hellogsmv3.domain.common.operation.entity.OperationTestResult;
import team.themoment.hellogsmv3.domain.common.operation.repository.OperationTestResultRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class QueryAnnounceTestResultService {

    private final OperationTestResultRepository operationTestResultRepository;

    @Transactional(readOnly = true)
    public AnnounceTestResultResDto execute() {
        OperationTestResult testResult = operationTestResultRepository.findTestResult()
                .orElseThrow(() -> new ExpectedException("시험 운영 정보를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        return AnnounceTestResultResDto.builder()
                .firstTestResultAnnouncementYn(testResult.getFirstTestResultAnnouncementYn())
                .secondTestResultAnnouncementYn(testResult.getSecondTestResultAnnouncementYn()).build();
    }
}
