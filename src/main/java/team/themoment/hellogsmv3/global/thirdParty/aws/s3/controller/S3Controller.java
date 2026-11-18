package team.themoment.hellogsmv3.global.thirdParty.aws.s3.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.dto.response.UploadImageResDto;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.service.UploadImageService;

@Tag(name = "Oneseo API", description = "원서 관련 API입니다.")
@RestController
@RequestMapping("/oneseo/v3")
@RequiredArgsConstructor
public class S3Controller {

    private final UploadImageService uploadImageService;

    @Operation(summary = "증명사진 업로드", description = "S3에 이미지를 업로드하고 공개 URL만 반환합니다. 원서에 자동 연결되지 않으므로 원서 저장 요청에 URL을 별도로 넣어야 합니다. 파일명 확장자 jpg, jpeg, png만 허용하며 대소문자를 구분하지 않습니다. Content-Type이나 매직바이트로 이미지 여부를 검사하지 않습니다. 최대 파일 크기는 5MB입니다.\n요청 가능 기간: 원서 접수 기간(시작·종료 시각 미포함)\n권한: APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "필수 file 파트 누락, 빈 파일, 확장자 없음/미지원 또는 5MB 초과", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족 또는 요청 가능 기간이 아님", content = @Content()),
            @ApiResponse(responseCode = "500", description = "S3 또는 입출력 오류", content = @Content())})
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, content = @Content(mediaType = "multipart/form-data", schema = @Schema(type = "object", requiredProperties = {
            "file"}), schemaProperties = @io.swagger.v3.oas.annotations.media.SchemaProperty(name = "file", schema = @Schema(type = "string", format = "binary", description = "증명사진 파일"))))
    @PostMapping(value = "/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadImageResDto uploadImage(
            @Parameter(description = "증명사진 파일(필수)", required = true, schema = @Schema(type = "string", format = "binary")) @RequestParam(value = "file") MultipartFile multipartFile) {
        return uploadImageService.execute(multipartFile);
    }
}
