package team.themoment.hellogsmv3.domain.common.operation.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.operation.dto.response.AnnounceTestResultResDto;
import team.themoment.hellogsmv3.domain.common.operation.service.AnnounceFirstTestResultService;
import team.themoment.hellogsmv3.domain.common.operation.service.AnnounceSecondTestResultService;
import team.themoment.hellogsmv3.domain.common.operation.service.QueryAnnounceTestResultService;
import team.themoment.sdk.response.CommonApiResponse;

@Tag(name = "Operation API", description = "결과 발표 관련 operation API입니다.")
@RestController
@RequestMapping("/operation/v3")
@RequiredArgsConstructor
public class OperationTestResultController {

    private final QueryAnnounceTestResultService queryAnnounceTestResultService;
    private final AnnounceFirstTestResultService announceFirstTestResultService;
    private final AnnounceSecondTestResultService announceSecondTestResultService;

    @Operation(summary = "결과 발표 여부 조회", description = "현재 결과 발표 여부를 조회합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "결과 발표 여부 조회 성공", content = @Content(schema = @Schema(implementation = AnnounceTestResultResDto.class))),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "ADMIN 또는 ROOT 권한이 필요합니다.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "시험 운영 정보를 찾을 수 없습니다.", content = @Content())})
    @GetMapping("/operation/status")
    public AnnounceTestResultResDto queryTestResult() {
        return queryAnnounceTestResultService.execute();
    }

    @Operation(summary = "1차 결과 발표", description = "1차 결과 발표 시각(firstResultsAnnouncement) 이후이며 모든 지원자의 1차 결과가 입력되어야 발표할 수 있습니다.\n권한: ADMIN, ROOT")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "1차 결과 발표 성공했습니다."),
            @ApiResponse(responseCode = "400", description = "1차 결과 발표 시각 이전, 1차 결과 미입력자 존재 또는 이미 1차 결과가 발표된 상태입니다.", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "ADMIN 또는 ROOT 권한이 필요합니다.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "시험 운영 정보를 찾을 수 없습니다.", content = @Content())})
    @PostMapping("/operation/announce-first-test-result")
    public CommonApiResponse announceFirstTestResult() {
        announceFirstTestResultService.execute();
        return CommonApiResponse.success("1차 결과 발표 성공했습니다.");
    }

    @Operation(summary = "2차 결과 발표", description = "최종 결과 발표 시각(finalResultsAnnouncement) 이후이며 1차 발표가 완료되어야 합니다. 모든 지원자의 1차 결과와 1차 합격자의 2차 결과가 입력되어야 합니다.\n권한: ADMIN, ROOT")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "2차 결과 발표 성공했습니다."),
            @ApiResponse(responseCode = "400", description = "최종 결과 발표 시각 이전, 1차 결과 미입력자 존재, 1차 합격자의 2차 결과 미입력, 1차 미발표 또는 이미 2차 결과가 발표된 상태입니다.", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인이 필요합니다.", content = @Content()),
            @ApiResponse(responseCode = "403", description = "ADMIN 또는 ROOT 권한이 필요합니다.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "시험 운영 정보를 찾을 수 없습니다.", content = @Content())})
    @PostMapping("/operation/announce-second-test-result")
    public CommonApiResponse announceSecondTestResult() {
        announceSecondTestResultService.execute();
        return CommonApiResponse.success("2차 결과 발표 성공했습니다.");
    }
}
