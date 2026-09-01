package team.themoment.hellogsmv3.domain.oneseo.dto.response;

/** 생활기록부 성적 추출 결과에 대한 검수 요청 사유 */
public enum ExtractionWarningType {
    /** 과목명을 표준 과목 목록에 매칭하지 못함 */
    UNRECOGNIZED_SUBJECT,
    /** 오타 보정 등으로 값을 유추함. 사용자 확인 필요 */
    LOW_CONFIDENCE,
    /** 해당 학기의 성적 표를 찾지 못함 */
    MISSING_SEMESTER,
    /** 특정 학기에서 해당 과목의 성취도를 찾지 못함 */
    MISSING_SUBJECT,
    /** 해당 학년의 출결 표를 찾지 못함 */
    MISSING_ATTENDANCE,
    /** 해당 학년의 봉사활동 시간을 찾지 못함 */
    MISSING_VOLUNTEER,
    /** 성취도가 허용 범위를 벗어남 */
    OUT_OF_RANGE
}
