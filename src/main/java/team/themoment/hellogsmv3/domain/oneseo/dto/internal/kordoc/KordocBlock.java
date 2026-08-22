package team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * kordoc {@code --format json} 출력의 블록입니다.
 *
 * <p>
 * {@code type}이 {@code "table"}이면 {@code table}이, 그 외(예: {@code "heading"},
 * {@code "text"})면 {@code text}가 채워집니다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KordocBlock(String type, String text, KordocTable table, int pageNumber) {

    public static final String TYPE_TABLE = "table";

    public boolean isTable() {
        return TYPE_TABLE.equals(type);
    }

    public List<List<KordocTableCell>> tableRowsOrEmpty() {
        return table == null ? List.of() : table.cellsOrEmpty();
    }

    public String textOrEmpty() {
        return text == null ? "" : text;
    }
}
