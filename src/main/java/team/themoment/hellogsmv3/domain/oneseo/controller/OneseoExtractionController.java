package team.themoment.hellogsmv3.domain.oneseo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.service.ExtractMiddleSchoolAchievementService;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;

@Tag(name = "Oneseo Extraction API", description = "생활기록부 성적 추출 관련 API입니다.")
@RestController
@RequestMapping("/oneseo/v3/extraction")
@RequiredArgsConstructor
public class OneseoExtractionController {

    private final ExtractMiddleSchoolAchievementService extractMiddleSchoolAchievementService;

    @Operation(summary = "생활기록부 성적 추출", description = "생활기록부 PDF에서 중학교 성적을 추출합니다. 파일을 서버에 저장하지 않고, 원서 데이터를 수정하지 않습니다. 추출 결과는 사용자가 검토한 뒤 원서 등록 API로 제출해야 합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "추출 성공"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 형식이거나, 텍스트 레이어가 없는 스캔 PDF인 경우")})
    @PostMapping("/middle-school-achievement")
    public ExtractedAchievementResDto extractMiddleSchoolAchievement(
            @Parameter(description = "생활기록부 PDF 파일") @RequestParam("file") MultipartFile file,
            @Parameter(description = "졸업 구분. 졸업예정자는 3학년 2학기 성적이 없습니다.") @RequestParam GraduationType graduationType,
            @Parameter(description = "자유학기제 또는 자유학년제. 예체능 성취점수 배열의 길이를 결정하므로 졸업예정자는 필수입니다.", schema = @Schema(allowableValues = {
                    "자유학기제", "자유학년제"})) @RequestParam(required = false) String liberalSystem,
            @AuthRequest Long memberId) {
        return extractMiddleSchoolAchievementService.execute(file, graduationType, liberalSystem, memberId);
    }
}
