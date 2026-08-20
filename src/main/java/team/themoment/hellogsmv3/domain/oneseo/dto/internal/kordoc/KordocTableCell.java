package team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * kordoc {@code --format json} 출력의 표 셀입니다.
 *
 * @param text
 *            셀 텍스트. 줄바꿈으로 구분된 여러 값이 한 셀에 뭉쳐 나올 수 있습니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KordocTableCell(String text, int colSpan, int rowSpan) {

    public String textOrEmpty() {
        return text == null ? "" : text;
    }
}
