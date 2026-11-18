package team.themoment.hellogsmv3.domain.oneseo.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ExtractMiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrDownloadUrlResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrUploadUrlResDto;
import team.themoment.hellogsmv3.domain.oneseo.service.ExtractMiddleSchoolAchievementService;
import team.themoment.hellogsmv3.domain.oneseo.service.IssueOcrDownloadUrlService;
import team.themoment.hellogsmv3.domain.oneseo.service.IssueOcrUploadUrlService;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;

@Tag(name = "Oneseo Extraction API", description = "생활기록부 성적 추출 관련 API입니다.")
@RestController
@RequestMapping("/oneseo/v3/extraction")
@RequiredArgsConstructor
public class OneseoExtractionController {

    private final ExtractMiddleSchoolAchievementService extractMiddleSchoolAchievementService;
    private final IssueOcrUploadUrlService issueOcrUploadUrlService;
    private final IssueOcrDownloadUrlService issueOcrDownloadUrlService;

    @Operation(summary = "생활기록부 성적 추출", description = "클라이언트가 추출한 생활기록부 텍스트를 중학교 성적으로 구조화합니다. 원본 파일은 전송받지 않고 원서도 수정하지 않습니다. 결과를 검토한 뒤 원서 등록·수정 API로 제출해야 합니다. strip 후 200자 이상이 필요하며 GED는 지원하지 않습니다.\n권한: APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "요청 검증 실패(공백 텍스트, source·graduationType 누락, pageCount 음수, 신뢰도 범위 오류, 잘못된 enum), strip 후 200자 미만, GED 또는 CANDIDATE의 자유학기제 구분 누락·오류", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @PostMapping("/middle-school-achievement")
    public ExtractedAchievementResDto extractMiddleSchoolAchievement(
            @RequestBody @Valid ExtractMiddleSchoolAchievementReqDto reqDto,
            @AuthRequest Long memberId) {
        return extractMiddleSchoolAchievementService.execute(reqDto, memberId);
    }

    @Operation(summary = "생활기록부 스캔 파일 업로드용 Presigned URL 발급", description = "클라이언트가 생활기록부 파일을 S3에 직접 PUT 업로드할 URL을 발급합니다. 유효시간은 5분이며 objectKey는 ocr-uploads/{memberId}/{uuid}.{ext}입니다. 기본 제한은 회원당 10분 고정 창에 30회 URL 발급 요청이며 환경 설정으로 변경할 수 있습니다. 잘못된 확장자는 집계하지 않고 429 요청은 집계합니다.\n권한: APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "fileExtension 누락 또는 지원하지 않는 확장자", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content()),
            @ApiResponse(responseCode = "429", description = "회원별 URL 발급 요청 제한 초과", content = @Content()),
            @ApiResponse(responseCode = "500", description = "S3 또는 내부 서비스 실패", content = @Content())})
    @PostMapping("/middle-school-achievement/ocr-upload-url")
    public OcrUploadUrlResDto issueOcrUploadUrl(
            @Parameter(description = "POST query 파라미터. pdf, jpg, jpeg, png(대소문자 무시). 점을 붙이면(.pdf) 400") @RequestParam String fileExtension,
            @AuthRequest Long memberId) {
        return issueOcrUploadUrlService.execute(memberId, fileExtension);
    }

    @Operation(summary = "생활기록부 스캔 파일 다운로드용 Presigned URL 발급", description = "클라이언트가 OCR 처리에 사용할 S3 다운로드 URL을 발급합니다. 유효시간은 1분이며 관리자도 요청자 자신의 ocr-uploads/{memberId}/ 접두사 객체만 조회할 수 있습니다. 30MB를 초과하면 S3 객체를 삭제하고 413을 반환합니다.\n권한: APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "objectKey 파라미터 누락", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족 또는 본인 소유의 objectKey가 아님", content = @Content()),
            @ApiResponse(responseCode = "404", description = "파일이 존재하지 않거나 만료됨", content = @Content()),
            @ApiResponse(responseCode = "413", description = "파일이 30MB를 초과하여 삭제됨", content = @Content())})
    @PostMapping("/middle-school-achievement/ocr-download-url")
    public OcrDownloadUrlResDto issueOcrDownloadUrl(
            @Parameter(description = "POST query 파라미터. 업로드 URL 발급 시 받은 본인 소유 objectKey") @RequestParam String objectKey,
            @AuthRequest Long memberId) {
        return issueOcrDownloadUrlService.execute(memberId, objectKey);
    }
}
