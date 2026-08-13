package team.themoment.hellogsmv3.domain.oneseo.service.extraction;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;

/**
 * 업로드된 생활기록부 파일에서 텍스트를 추출하는 포트입니다.
 *
 * <p>
 * 현재 구현체는 PDF 텍스트 레이어를 직접 읽는 방식 하나뿐입니다. 추후 스캔 이미지 폴백이 필요해지면 OCR 기반 구현체를 추가하고,
 * 파싱 로직({@link MiddleSchoolRecordParser})은 그대로 재사용합니다.
 */
public interface AchievementTextExtractor {

    /**
     * @param content
     *            원본 파일 바이트
     * @param contentType
     *            MIME 타입
     * @return 추출된 텍스트와 진단 정보
     */
    ExtractedTextDto extract(byte[] content, String contentType);

    /** 이 구현체가 해당 MIME 타입을 처리할 수 있는지 여부 */
    boolean supports(String contentType);
}
