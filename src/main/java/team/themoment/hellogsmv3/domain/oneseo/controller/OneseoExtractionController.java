package team.themoment.hellogsmv3.domain.oneseo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ExtractMiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrUploadUrlResDto;
import team.themoment.hellogsmv3.domain.oneseo.service.ExtractMiddleSchoolAchievementService;
import team.themoment.hellogsmv3.domain.oneseo.service.IssueOcrUploadUrlService;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;

@Tag(name = "Oneseo Extraction API", description = "생활기록부 성적 추출 관련 API입니다.")
@RestController
@RequestMapping("/oneseo/v3/extraction")
@RequiredArgsConstructor
public class OneseoExtractionController {

    private final ExtractMiddleSchoolAchievementService extractMiddleSchoolAchievementService;
    private final IssueOcrUploadUrlService issueOcrUploadUrlService;

    @Operation(summary = "생활기록부 성적 추출", description = "클라이언트가 생활기록부에서 뽑아 보낸 텍스트를 중학교 성적으로 구조화합니다. 원본 파일은 서버로 전송되지 않으며, 원서 데이터를 수정하지 않습니다. 추출 결과는 사용자가 검토한 뒤 원서 등록 API로 제출해야 합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "추출 성공"),
            @ApiResponse(responseCode = "400", description = "텍스트가 너무 짧거나, 검정고시 지원자이거나, 졸업예정자인데 자유학기제 구분이 없는 경우")})
    @PostMapping("/middle-school-achievement")
    public ExtractedAchievementResDto extractMiddleSchoolAchievement(
            @RequestBody @Valid ExtractMiddleSchoolAchievementReqDto reqDto,
            @AuthRequest Long memberId) {
        return extractMiddleSchoolAchievementService.execute(reqDto, memberId);
    }

    @Operation(summary = "생활기록부 스캔 파일 업로드용 Presigned URL 발급", description = "프론트가 kordoc OCR을 실행하기 전, 생활기록부 스캔 파일을 S3에 직접 업로드하기 위해 호출합니다. 이 API는 회원별 OCR 요청 횟수 제한을 확인한 뒤 통과 시에만 Presigned URL을 발급합니다. 발급받은 URL로 파일을 S3에 직접 업로드하고, 이후 OCR 실행 요청에는 파일 대신 objectKey를 전달해야 합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "발급 성공"),
            @ApiResponse(responseCode = "400", description = "지원하지 않는 파일 확장자인 경우"),
            @ApiResponse(responseCode = "429", description = "최근 OCR 요청이 너무 많은 경우")})
    @PostMapping("/middle-school-achievement/ocr-upload-url")
    public OcrUploadUrlResDto issueOcrUploadUrl(
            @Parameter(description = "업로드할 파일의 확장자 (pdf, jpg, jpeg, png)") @RequestParam String fileExtension,
            @AuthRequest Long memberId) {
        return issueOcrUploadUrlService.execute(memberId, fileExtension);
    }
}
