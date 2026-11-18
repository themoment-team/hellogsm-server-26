package team.themoment.hellogsmv3.domain.oneseo.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
public record OcrDownloadUrlResDto(
        @Schema(description = "S3 다운로드 URL. 발급 후 1분 유효", format = "uri", example = "https://example.com/file") String downloadUrl) {
}
