package kr.hellogsm.entrance.lambda

import kr.hellogsm.entrance.engine.scoring.RawRecordMapping
import kr.hellogsm.entrance.engine.scoring.StudentRecord
import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.SemesterRef

/**
 * [ScoreCalculatorRequest](server가 보내는 원시 성적 입력) → `StudentRecord`(엔진 scoring 입력) 변환.
 *
 * 성취도 코드 환산·출결 환산 같은 **해석 규칙은 [RawRecordMapping]** 에 있다 —
 * `entrance-batch`의 `StudentRecordMapper`와 규칙이 갈라지지 않도록 공유하며, 이 매퍼는
 * 요청 DTO에서 값을 꺼내 넘기는 일만 한다.
 *
 * 결측 학기 대체는 매퍼가 아니라 `ScoringEngine`(plan에 선언된 `MissingSemesterStrategy`)의
 * 책임이다 — 그래서 plan이 직접 채점하지 않는 학기(1-1)도 **대체 원본으로 쓰일 수 있으므로
 * submitted map에 그대로 넣는다.** 1-1을 여기서 미리 걸러내면 엔진이 SAME_YEAR_OTHER_SEMESTER
 * 전략으로 1-2를 채울 방법이 없어진다 (Plan.kt의 `missingSemester(SAME_YEAR_OTHER_SEMESTER, ...)`
 * 선언이 있어도 무력화됨).
 *
 * 같은 이유로 대부분의 DTO는 achievement1_1을 그대로 포함시키며, 실제 대체 사용 여부는
 * 최종 점수 계산(대체 로직) 단계에서만 가려진다. 점수 계산이 끝난 뒤 학기별 성적을 그대로
 * 전달하는 응답이라 하더라도 1-1은 계속 포함시켜야 한다 — 호출부는 1-1이 대체 원본으로
 * 실제 사용되었는지 여부를 알 수 없기 때문이다.
 */
object StudentRecordMapper {

    fun toStudentRecord(request: ScoreCalculatorRequest): StudentRecord {
        val graduationType = requireNotNull(request.graduationType) { "graduationType이 없음" }
            .let(::toPlanGraduationType)

        if (graduationType == GraduationType.GED) {
            return StudentRecord.Ged(
                averageScore = requireNotNull(request.gedAvgScore) { "검정고시 지원자에 gedAvgScore가 없음" },
            )
        }

        return StudentRecord.Transcript(
            graduationType = graduationType,
            generalAchievements = RawRecordMapping.generalAchievements(
                mapOf(
                    SemesterRef(1, 1) to request.achievement1_1,
                    SemesterRef(1, 2) to request.achievement1_2,
                    SemesterRef(2, 1) to request.achievement2_1,
                    SemesterRef(2, 2) to request.achievement2_2,
                    SemesterRef(3, 1) to request.achievement3_1,
                    SemesterRef(3, 2) to request.achievement3_2,
                ),
            ),
            artsAchievements = RawRecordMapping.achievements(request.artsPhysicalAchievement),
            attendanceByYear = RawRecordMapping.attendanceByYear(
                absentDays = request.absentDays,
                attendanceDays = request.attendanceDays,
            ),
            volunteerHoursByYear = RawRecordMapping.volunteerHoursByYear(request.volunteerTime),
        )
    }

    private fun toPlanGraduationType(raw: String): GraduationType = when (raw) {
        "CANDIDATE" -> GraduationType.CANDIDATE
        "GRADUATE" -> GraduationType.GRADUATE
        "GED" -> GraduationType.GED
        else -> throw IllegalArgumentException("알 수 없는 졸업 구분: $raw")
    }
}
