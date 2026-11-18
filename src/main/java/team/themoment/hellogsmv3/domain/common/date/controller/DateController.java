package team.themoment.hellogsmv3.domain.common.date.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.date.dto.DateResDto;
import team.themoment.hellogsmv3.domain.common.date.service.QueryDateService;

@Tag(name = "Date API", description = "일정 관련 API입니다.")
@RestController
@RequiredArgsConstructor
public class DateController {

    private final QueryDateService queryDateService;

    @Operation(summary = "일정 조회", description = "환경변수에 설정된 입학 전형 일정 6개를 반환합니다. 인증 없이 호출할 수 있습니다. 각 값은 KST(Asia/Seoul) 로컬 시각에 리터럴 Z를 붙인 yyyy-MM-dd'T'HH:mm:ss'Z' 형식이며 UTC 변환값이 아닙니다.")
    @ApiResponse(responseCode = "200", description = "일정 조회 성공", content = @Content(schema = @Schema(implementation = DateResDto.class)))
    @GetMapping("/date")
    public DateResDto getDate() {
        return queryDateService.execute();
    }
}
