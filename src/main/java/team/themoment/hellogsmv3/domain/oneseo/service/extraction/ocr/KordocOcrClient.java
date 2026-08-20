package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import java.nio.file.Path;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocParseResult;

/** kordoc으로 스캔 이미지 한 장을 인식해 표 재구성 결과를 얻습니다. */
public interface KordocOcrClient {

    KordocParseResult recognize(Path imagePath);
}
