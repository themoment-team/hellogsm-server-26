package team.themoment.hellogsmv3.domain.common.operation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.operation.dto.response.AnnounceTestResultResDto;
import team.themoment.hellogsmv3.domain.common.operation.service.AnnounceFirstTestResultService;
import team.themoment.hellogsmv3.domain.common.operation.service.AnnounceSecondTestResultService;
import team.themoment.hellogsmv3.domain.common.operation.service.QueryAnnounceTestResultService;
import team.themoment.hellogsmv3.global.common.response.CommonApiResponse;

@Tag(name = "Operation API", description = "결과 발표 관련 operation API입니다.")
@RestController
@RequestMapping("/operation/v3")
@RequiredArgsConstructor
public class OperationTestResultController {

    private final QueryAnnounceTestResultService queryAnnounceTestResultService;
    private final AnnounceFirstTestResultService announceFirstTestResultService;
    private final AnnounceSecondTestResultService announceSecondTestResultService;

    @Operation(summary = "결과 발표 여부 조회", description = "현재 결과 발표 여부를 조회합니다.")
    @GetMapping("/operation/status")
    public AnnounceTestResultResDto queryTestResult() {
        return queryAnnounceTestResultService.execute();
    }

    @Operation(summary = "1차 결과 발표", description = "1차 결과를 발표합니다.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "1차 결과 발표 성공했습니다."),
            @ApiResponse(responseCode = "400", description = "1차 결과 발표 기간 이전이거나 이미 발표된 상태입니다.", content = @Content())})
    @PostMapping("/operation/announce-first-test-result")
    public CommonApiResponse announceFirstTestResult() {
        announceFirstTestResultService.execute();
        return CommonApiResponse.success("1차 결과 발표 성공했습니다.");
    }

    @Operation(summary = "2차 결과 발표", description = "2차 결과를 발표합니다.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "2차 결과 발표 성공했습니다."),
            @ApiResponse(responseCode = "400", description = "2차 결과 발표 기간 이전이거나 이미 발표된 상태입니다.", content = @Content())})
    @PostMapping("/operation/announce-second-test-result")
    public CommonApiResponse announceSecondTestResult() {
        announceSecondTestResultService.execute();
        return CommonApiResponse.success("2차 결과 발표 성공했습니다.");
    }
}
