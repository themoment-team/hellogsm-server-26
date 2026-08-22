package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import team.themoment.sdk.exception.ExpectedException;

/**
 * kordoc subprocess 동시 실행 개수를 제한합니다.
 *
 * <p>
 * kordoc은 페이지당 약 1초의 CPU를 씁니다. 원서 접수 마감 직전처럼 요청이 몰리면 프로세스가 무제한으로 늘어날 수 있어, 초과분은
 * 큐잉 대신 즉시 재시도를 요청합니다.
 */
@Component
public class OcrConcurrencyGate {

    private final Semaphore semaphore;
    private final long acquireTimeoutSeconds;

    public OcrConcurrencyGate(@Value("${oneseo.extraction.ocr.max-concurrent-jobs:2}") int maxConcurrentJobs,
            @Value("${oneseo.extraction.ocr.acquire-timeout-seconds:5}") long acquireTimeoutSeconds) {
        this.semaphore = new Semaphore(maxConcurrentJobs);
        this.acquireTimeoutSeconds = acquireTimeoutSeconds;
    }

    public <T> T runWithinLimit(Supplier<T> action) {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(acquireTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExpectedException("OCR 처리 대기가 중단되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (!acquired) {
            throw new ExpectedException("OCR 처리가 지금 많이 몰려 있습니다. 잠시 후 다시 시도해주세요.", HttpStatus.SERVICE_UNAVAILABLE);
        }

        try {
            return action.get();
        } finally {
            semaphore.release();
        }
    }
}
