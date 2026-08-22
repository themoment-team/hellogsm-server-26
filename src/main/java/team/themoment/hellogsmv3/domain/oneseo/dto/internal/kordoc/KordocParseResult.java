package team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * kordoc {@code --format json} 출력 최상위 구조입니다. 문서 전체가 아니라 페이지 1장짜리 입력만 다루므로 필요한
 * 필드만 매핑합니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KordocParseResult(List<KordocBlock> blocks) {

    public List<KordocBlock> blocksOrEmpty() {
        return blocks == null ? List.of() : blocks;
    }
}
