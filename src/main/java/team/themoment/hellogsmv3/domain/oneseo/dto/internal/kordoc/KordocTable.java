package team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * kordoc {@code --format json} 출력의 표 블록입니다.
 *
 * <p>
 * {@code cells}는 행 단위로 묶인 2차원 배열({@code IRCell[][]})입니다. 별도로 셀을 감싸는 "row" 객체는
 * 존재하지 않습니다. {@code rows}(행 개수), {@code cols}, {@code hasHeader} 등은 이 서비스에서 쓰지
 * 않아 매핑하지 않습니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KordocTable(List<List<KordocTableCell>> cells) {

    public List<List<KordocTableCell>> cellsOrEmpty() {
        if (cells == null) {
            return List.of();
        }
        return cells.stream().map(row -> row == null ? List.<KordocTableCell>of() : row).toList();
    }
}
