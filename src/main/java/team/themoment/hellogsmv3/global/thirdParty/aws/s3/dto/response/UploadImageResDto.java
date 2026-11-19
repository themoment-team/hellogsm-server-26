package team.themoment.hellogsmv3.global.thirdParty.aws.s3.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UploadImageResDto(
        @Schema(description = "업로드된 이미지의 공개 URL. 원서 저장 요청에 별도로 전달합니다.", example = "https://example-bucket.s3.amazonaws.com/photo.jpg", requiredMode = Schema.RequiredMode.REQUIRED, format = "uri") String url) {
}
