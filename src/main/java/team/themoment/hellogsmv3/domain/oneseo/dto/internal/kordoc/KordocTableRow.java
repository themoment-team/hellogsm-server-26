package team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KordocTableRow(List<KordocTableCell> cells) {

    public List<KordocTableCell> cellsOrEmpty() {
        return cells == null ? List.of() : cells;
    }
}
