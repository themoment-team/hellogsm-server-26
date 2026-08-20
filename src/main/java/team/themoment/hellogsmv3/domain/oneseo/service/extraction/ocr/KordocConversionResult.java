package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import java.util.List;

/**
 * kordoc 표 재구성 결과를
 * {@link team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser}가
 * 읽는 줄 형식으로 바꾼 결과입니다.
 *
 * @param rawText
 *            변환된 줄들을 개행으로 이어붙인 텍스트
 * @param unrecognizedSubjectBlobs
 *            표준 과목 9개만으로 분해하지 못해 건너뛴 과목명 원문. 사용자 검수가 필요합니다.
 */
public record KordocConversionResult(String rawText, List<String> unrecognizedSubjectBlobs) {
}
