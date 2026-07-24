package kr.hellogsm.entrance.lambda

import kr.hellogsm.entrance.engine.scoring.AttendanceRecord
import kr.hellogsm.entrance.engine.scoring.StudentRecord
import kr.hellogsm.entrance.plan.Achievement
import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.SemesterRef

/**
 * [ScoreCalculatorRequest](server가 보내는 원시 성적 입력) → `StudentRecord`(엔진 scoring 입력) 변환.
 *
 * 결측 학기 대체는 이 매퍼가 아니라 `ScoringEngine`(plan에 선언된 `MissingSemesterStrategy`)의
 * 책임이다 — 그래서 plan이 직접 채점하지 않는 학기(1-1)도 **대체 원본으로 쓰일 수 있으므로
 * submitted map에 그대로 넣는다.** 1-1을 여기서 미리 걸러내면 엔진이 SAME_YEAR_OTHER_SEMESTER
 * 전략으로 1-2를 채울 방법이 없어진다 (Plan2026의 `missingSemester(SAME_YEAR_OTHER_SEMESTER, ...)`
 * 선언이 있어도 무력화됨). `entrance-batch`의 `StudentRecordMapper`는 영속 엔티티에 애초에
 * achievement1_1 컬럼이 없어 이 대체를 받을 수 없다 — 별도 이슈(스키마 확장 필요).
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
            generalAchievements = buildMap {
                putSemester(SemesterRef(1, 1), request.achievement1_1)
                putSemester(SemesterRef(1, 2), request.achievement1_2)
                putSemester(SemesterRef(2, 1), request.achievement2_1)
                putSemester(SemesterRef(2, 2), request.achievement2_2)
                putSemester(SemesterRef(3, 1), request.achievement3_1)
                putSemester(SemesterRef(3, 2), request.achievement3_2)
            },
            artsAchievements = request.artsPhysicalAchievement.toAchievements(),
            attendanceByYear = toAttendanceByYear(
                absentDays = request.absentDays.orEmpty(),
                attendanceDays = request.attendanceDays.orEmpty(),
            ),
            volunteerHoursByYear = request.volunteerTime.orEmpty()
                .mapIndexed { index, hours -> (index + 1) to hours }
                .toMap(),
        )
    }

    private fun MutableMap<SemesterRef, List<Achievement>>.putSemester(
        semester: SemesterRef,
        raw: List<Int>?,
    ) {
        val achievements = raw.toAchievements()
        if (achievements.isNotEmpty()) put(semester, achievements)
    }

    /** 0(미수강)을 제외하고 5→A … 1→E 로 변환한다. */
    private fun List<Int>?.toAchievements(): List<Achievement> =
        this.orEmpty().filter { it != 0 }.map { it.toAchievement() }

    private fun Int.toAchievement(): Achievement = when (this) {
        5 -> Achievement.A
        4 -> Achievement.B
        3 -> Achievement.C
        2 -> Achievement.D
        1 -> Achievement.E
        else -> throw IllegalArgumentException("유효하지 않은 성취도 환산점수: $this (허용 1..5, 0=미수강)")
    }

    private fun toAttendanceByYear(
        absentDays: List<Int>,
        attendanceDays: List<Int>,
    ): Map<Int, AttendanceRecord> =
        (1..3).associateWith { year ->
            val base = (year - 1) * 3
            val latenessTotal = (0 until 3).sumOf { attendanceDays.getOrElse(base + it) { 0 } }
            AttendanceRecord(
                absenceDays = absentDays.getOrElse(year - 1) { 0 },
                latenessCount = latenessTotal,
                earlyLeaveCount = 0,
                classAbsenceCount = 0,
            )
        }

    private fun toPlanGraduationType(raw: String): GraduationType = when (raw) {
        "CANDIDATE" -> GraduationType.CANDIDATE
        "GRADUATE" -> GraduationType.GRADUATE
        "GED" -> GraduationType.GED
        else -> throw IllegalArgumentException("알 수 없는 졸업 구분: $raw")
    }
}
