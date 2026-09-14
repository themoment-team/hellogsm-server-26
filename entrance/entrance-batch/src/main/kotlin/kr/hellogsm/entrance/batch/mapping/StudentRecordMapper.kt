package kr.hellogsm.entrance.batch.mapping

import kr.hellogsm.entrance.engine.scoring.RawRecordMapping
import kr.hellogsm.entrance.engine.scoring.StudentRecord
import kr.hellogsm.entrance.plan.GraduationType as PlanGraduationType
import kr.hellogsm.entrance.plan.SemesterRef
import team.themoment.hellogsmv3.domain.oneseo.entity.MiddleSchoolAchievement
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType as EntityGraduationType

/**
 * `MiddleSchoolAchievement`(서버 JPA 엔티티) → `StudentRecord`(엔진 scoring 입력) 변환.
 *
 * 성취도 코드 환산·출결 환산 같은 **해석 규칙은 [RawRecordMapping]** 에 있다 —
 * `entrance-lambda`의 `StudentRecordMapper`와 규칙이 갈라지지 않도록 공유하며, 이 매퍼는
 * 엔티티에서 값을 꺼내 넘기는 일만 한다.
 *
 * 결측 학기 대체는 매퍼가 아니라 `ScoringEngine`(plan에 선언된 `MissingSemesterStrategy`)의
 * 책임이다 — `achievement1_1`도 plan이 직접 채점하지는 않지만 `SAME_YEAR_OTHER_SEMESTER` 전략의
 * 대체 원본으로 쓰일 수 있어 그대로 submitted map에 넣는다. server는 더 이상 접수 시점에
 * 결측 학기를 미리 채우지 않으므로(`OneseoService.buildCalcDto`), 이 매퍼가 넘기는 원본 그대로
 * 최종 채점 시점에 대체가 일어난다.
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
            generalAchievements = RawRecordMapping.generalAchievements(
                mapOf(
                    SemesterRef(1, 1) to achievement.achievement1_1,
                    SemesterRef(1, 2) to achievement.achievement1_2,
                    SemesterRef(2, 1) to achievement.achievement2_1,
                    SemesterRef(2, 2) to achievement.achievement2_2,
                    SemesterRef(3, 1) to achievement.achievement3_1,
                    SemesterRef(3, 2) to achievement.achievement3_2,
                ),
            ),
            artsAchievements = RawRecordMapping.achievements(achievement.artsPhysicalAchievement),
            attendanceByYear = RawRecordMapping.attendanceByYear(
                absentDays = achievement.absentDays,
                attendanceDays = achievement.attendanceDays,
            ),
            volunteerHoursByYear = RawRecordMapping.volunteerHoursByYear(achievement.volunteerTime),
        )
    }

    private fun EntityGraduationType.toPlan(): PlanGraduationType = when (this) {
        EntityGraduationType.CANDIDATE -> PlanGraduationType.CANDIDATE
        EntityGraduationType.GRADUATE -> PlanGraduationType.GRADUATE
        EntityGraduationType.GED -> PlanGraduationType.GED
    }
}
