package team.themoment.hellogsmv3.domain.oneseo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OneseoApplyEvent {
    private String name;
    private String summitCode;
    private GraduationType graduationType;
    private Screening screening;
}
