package kr.hellogsm.entrance.batch.mapping

import kr.hellogsm.entrance.engine.scoring.AttendanceRecord
import kr.hellogsm.entrance.engine.scoring.StudentRecord
import kr.hellogsm.entrance.plan.Achievement
import kr.hellogsm.entrance.plan.GraduationType as PlanGraduationType
import kr.hellogsm.entrance.plan.SemesterRef
import team.themoment.hellogsmv3.domain.oneseo.entity.MiddleSchoolAchievement
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType as EntityGraduationType

/**
 * `MiddleSchoolAchievement`(서버 JPA 엔티티) → `StudentRecord`(엔진 scoring 입력) 변환.
 *
 * go-hellogsm-score-calculator(scoring parity 기준 커밋 d7b65b4)의 입력 해석을 그대로 재현한다:
 * - 성취도 환산점수 5→A … 1→E, **0(미수강)은 제외**한다(go 는 0 을 과목 수에서 뺀다).
 * - 출결: `absentDays`(학년별 결석 3개) + `attendanceDays`(학년별 지각·조퇴·결과 = 9개).
 *   go 는 결석합 + (지각·조퇴·결과 합 ÷ 3) 을 환산 결석으로 쓰는데, 엔진 `AttendanceRecord` 는
 *   학년 합산 후 `latenessPerAbsenceDay`(=3) 로 나누므로, 지각·조퇴·결과를 latenessCount 하나로
 *   합쳐도 점수가 동일하다. 학년 3개를 모두 채워 결측 학년 기본점 경로를 타지 않게 한다.
 * - 봉사: `volunteerTime`(학년별 3개).
 */
object StudentRecordMapper {

    fun toStudentRecord(
        achievement: MiddleSchoolAchievement,
        graduationType: EntityGraduationType,
    ): StudentRecord {
        val plan = graduationType.toPlan()
        if (plan == PlanGraduationType.GED) {
            return StudentRecord.Ged(
                averageScore = requireNotNull(achievement.gedAvgScore) {
                    "검정고시 지원자에 gedAvgScore 가 없음: oneseoId=${achievement.id}"
                },
            )
        }

        return StudentRecord.Transcript(
            graduationType = plan,
            generalAchievements = buildMap {
                putSemester(SemesterRef(1, 2), achievement.achievement1_2)
                putSemester(SemesterRef(2, 1), achievement.achievement2_1)
                putSemester(SemesterRef(2, 2), achievement.achievement2_2)
                putSemester(SemesterRef(3, 1), achievement.achievement3_1)
                putSemester(SemesterRef(3, 2), achievement.achievement3_2)
            },
            artsAchievements = achievement.artsPhysicalAchievement.toAchievements(),
            attendanceByYear = toAttendanceByYear(
                absentDays = achievement.absentDays.orEmpty(),
                attendanceDays = achievement.attendanceDays.orEmpty(),
            ),
            volunteerHoursByYear = achievement.volunteerTime.orEmpty()
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

    private fun EntityGraduationType.toPlan(): PlanGraduationType = when (this) {
        EntityGraduationType.CANDIDATE -> PlanGraduationType.CANDIDATE
        EntityGraduationType.GRADUATE -> PlanGraduationType.GRADUATE
        EntityGraduationType.GED -> PlanGraduationType.GED
    }
}
