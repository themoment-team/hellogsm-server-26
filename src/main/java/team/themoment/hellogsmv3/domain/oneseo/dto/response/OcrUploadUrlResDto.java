package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
public record OcrUploadUrlResDto(
        @Schema(description = "S3 직접 PUT 업로드 URL. 발급 후 5분 유효", format = "uri", example = "https://example.com/file") String uploadUrl,
        @Schema(description = "업로드 객체 키. ocr-uploads/{memberId}/{uuid}.{ext} 형식") String objectKey) {
}
