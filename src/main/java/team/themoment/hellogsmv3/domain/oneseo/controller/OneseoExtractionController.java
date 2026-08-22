package team.themoment.hellogsmv3.domain.oneseo.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.ExtractMiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrPageTextResDto;
import team.themoment.hellogsmv3.domain.oneseo.service.ExtractMiddleSchoolAchievementService;
import team.themoment.hellogsmv3.domain.oneseo.service.ExtractOcrPageTextService;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;

@Tag(name = "Oneseo Extraction API", description = "생활기록부 성적 추출 관련 API입니다.")
@RestController
@RequestMapping("/oneseo/v3/extraction")
@RequiredArgsConstructor
public class OneseoExtractionController {

    private final ExtractMiddleSchoolAchievementService extractMiddleSchoolAchievementService;
    private final ExtractOcrPageTextService extractOcrPageTextService;

    @Operation(summary = "생활기록부 성적 추출", description = "클라이언트가 생활기록부에서 뽑아 보낸 텍스트를 중학교 성적으로 구조화합니다. 원본 파일은 서버로 전송되지 않으며, 원서 데이터를 수정하지 않습니다. 추출 결과는 사용자가 검토한 뒤 원서 등록 API로 제출해야 합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "추출 성공"),
            @ApiResponse(responseCode = "400", description = "텍스트가 너무 짧거나, 검정고시 지원자이거나, 졸업예정자인데 자유학기제 구분이 없는 경우")})
    @PostMapping("/middle-school-achievement")
    public ExtractedAchievementResDto extractMiddleSchoolAchievement(
            @RequestBody @Valid ExtractMiddleSchoolAchievementReqDto reqDto,
            @AuthRequest Long memberId) {
        return extractMiddleSchoolAchievementService.execute(reqDto, memberId);
    }

    @Operation(summary = "생활기록부 스캔 페이지 OCR", description = "텍스트 레이어가 없는 페이지 한 장의 스캔 이미지를 서버로 받아 kordoc으로 인식하고, 위 성적 추출 API가 읽는 rawText 줄 형식으로 되돌려줍니다. 업로드된 이미지는 인식 처리 동안만 임시로 존재하며 처리 직후 삭제되고 서버에 저장되지 않습니다. 반환된 rawText는 클라이언트가 문서 전체 텍스트에서 이 페이지가 있던 자리에 이어붙인 뒤 위 성적 추출 API로 제출해야 합니다.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "인식 성공"),
            @ApiResponse(responseCode = "400", description = "파일이 없거나 지원하지 않는 확장자인 경우"),
            @ApiResponse(responseCode = "503", description = "OCR 처리가 몰려 있어 즉시 처리할 수 없는 경우. 잠시 후 재시도")})
    @PostMapping(value = "/middle-school-achievement/ocr-page", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OcrPageTextResDto extractOcrPageText(
            @Parameter(description = "스캔 페이지 이미지 (jpg, jpeg, png)") @RequestParam("file") MultipartFile file) {
        return extractOcrPageTextService.execute(file);
    }
}
