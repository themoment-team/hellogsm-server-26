package team.themoment.hellogsmv3.domain.common.date.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.date.dto.DateResDto;
import team.themoment.hellogsmv3.domain.common.date.service.QueryDateService;

@Tag(name = "Date API", description = "일정 관련 API입니다.")
@RestController
@RequiredArgsConstructor
public class DateController {

    private final QueryDateService queryDateService;

    @Operation(summary = "일정 조회", description = "FE에서 페이지 트리거가 되는 date를 반환합니다.")
    @GetMapping("/date")
    public DateResDto getDate() {
        return queryDateService.execute();
    }
}
