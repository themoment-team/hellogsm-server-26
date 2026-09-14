package team.themoment.hellogsmv3.domain.oneseo.dto.internal;

/** 텍스트를 어느 경로로 얻었는지 나타냅니다. */
public enum ExtractionSource {
    /** PDF에 들어있던 텍스트 레이어를 직접 읽음 */
    TEXT_LAYER,
    /** 페이지를 이미지로 렌더링한 뒤 광학 인식 */
    OCR
}
