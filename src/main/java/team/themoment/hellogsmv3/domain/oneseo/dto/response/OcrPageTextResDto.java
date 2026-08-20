package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/** 스캔 페이지 한 장을 kordoc으로 인식해 되돌린 rawText 조각입니다. */
@Builder
public record OcrPageTextResDto(
        @Schema(description = "이 페이지에서 재구성한 rawText 줄들. 문서 전체 rawText에서 이 페이지가 있던 위치에 그대로 이어붙이면 됩니다.") String rawText,
        @Schema(description = "표준 과목 9개만으로 분해하지 못해 건너뛴 과목명 원문 목록. 비어 있지 않으면 직접 확인이 필요합니다.") List<String> unrecognizedSubjectBlobs) {
}
