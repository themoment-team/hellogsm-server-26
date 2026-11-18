package team.themoment.hellogsmv3.domain.oneseo.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;

/**
 * 클라이언트가 생활기록부에서 뽑아 보낸 텍스트입니다.
 *
 * <p>
 * 파일에서 텍스트를 얻는 일은 클라이언트가 담당하고, 서버는 그 텍스트를 성적으로 구조화하는 일만 맡습니다. 원본 파일은 서버로 오지
 * 않으므로 성적 · 출결 · 봉사에 해당하는 부분만 잘라 보내는 것을 권장합니다. 다만 학년을 배정하는 데 {@code [N학년]} 줄이
 * 필요하므로 이 줄은 반드시 포함해야 합니다.
 */
public record ExtractMiddleSchoolAchievementReqDto(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "생활기록부에서 추출한 텍스트. 줄바꿈과 [N학년] 줄을 유지해야 하며 strip 후 200자 이상이어야 합니다.") @NotBlank String rawText,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "원본 PDF 페이지 수. 진단용이며 파싱에는 쓰이지 않습니다.", example = "17") @PositiveOrZero int pageCount,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "원본 PDF에 텍스트 레이어가 있었는지 여부") boolean hasTextLayer,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "텍스트를 얻은 경로. TEXT_LAYER 또는 OCR") @NotNull ExtractionSource source,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "글자를 얼마나 확신하며 읽었는지 0.0 ~ 1.0. 비워두면 경로에 따른 기본값을 씁니다.", example = "0.78", nullable = true) @DecimalMin("0.0") @DecimalMax("1.0") Double recognitionConfidence,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "졸업 구분. CANDIDATE·GRADUATE만 추출 가능하며 GED는 400입니다. 졸업예정자는 3학년 2학기 성적이 없습니다.") @NotNull GraduationType graduationType,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, description = "CANDIDATE는 자유학기제 또는 자유학년제 필수. GRADUATE는 생략·임의 문자열도 허용하며 자유학기제가 아니면 자유학년제로 처리합니다.", nullable = true) String liberalSystem) {
}
