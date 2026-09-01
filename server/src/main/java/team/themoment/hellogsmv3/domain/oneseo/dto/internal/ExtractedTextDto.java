package team.themoment.hellogsmv3.domain.oneseo.dto.internal;

/**
 * 파일에서 추출한 원문 텍스트입니다.
 *
 * @param rawText
 *            추출된 전체 텍스트
 * @param pageCount
 *            원본 페이지 수
 * @param hasTextLayer
 *            원본 PDF에 텍스트 레이어가 있었는지 여부. OCR로 추출한 경우에도 원본 사실을 그대로 담습니다.
 * @param source
 *            실제로 텍스트를 얻은 경로
 * @param recognitionConfidence
 *            글자를 얼마나 확신하며 읽었는지 0.0 ~ 1.0. 텍스트 레이어에서 그대로 읽은 경우는 추측이 없으므로 1.0입니다.
 *            OCR은 엔진이 보고한 값을 씁니다.
 */
public record ExtractedTextDto(String rawText, int pageCount, boolean hasTextLayer, ExtractionSource source,
        double recognitionConfidence) {
}
