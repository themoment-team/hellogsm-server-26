package kr.hellogsm.entrance.engine.scoring

import kr.hellogsm.entrance.plan.Achievement
import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.SemesterRef
import java.math.BigDecimal

/**
 * 지원자가 제출한 성적 원본.
 *
 * plan(요강)이 아닌 지원자 개인의 입력이며, [ScoringEngine]이 plan의 산출 규칙에 따라
 * 환산점수로 변환한다. 자료가 없는 학기/학년은 키를 넣지 않는다 — 결측 처리(대체 학기,
 * 기본점 부여)는 엔진이 plan의 규칙대로 수행한다.
 */
sealed interface StudentRecord {
    val graduationType: GraduationType

    /** 내신 성적 원본 (졸업예정자/졸업자) */
    data class Transcript(
        override val graduationType: GraduationType,
        /**
         * 학기별 일반교과 성취도 목록. 성적이 없는 학기는 키를 넣지 않는다.
         * plan의 반영 학기 외 학기(예: 1-1)도 넣을 수 있으며, 결측 학기 대체 시 참조된다.
         */
        val generalAchievements: Map<SemesterRef, List<Achievement>>,
        /** 예체능(체육·음악·미술) 교과 성취도 목록 — 3년간 전체, 학기 구분 없음 */
        val artsAchievements: List<Achievement>,
        /** 학년별 미인정 출결 횟수. 출결 자료가 없는 학년은 키를 넣지 않는다 */
        val attendanceByYear: Map<Int, AttendanceRecord>,
        /** 학년별 연간 봉사활동 시간. 봉사 자료가 없는 학년은 키를 넣지 않는다 */
        val volunteerHoursByYear: Map<Int, Int>,
    ) : StudentRecord {
        init {
            require(graduationType != GraduationType.GED) { "검정고시 합격자는 StudentRecord.Ged를 사용해야 함" }
            volunteerHoursByYear.forEach { (year, hours) ->
                require(hours >= 0) { "봉사활동 시간은 음수일 수 없음: $year 학년 $hours 시간" }
            }
        }
    }

    /** 검정고시 성적 원본 */
    data class Ged(
        /** 검정고시 전 과목 평균점수 */
        val averageScore: BigDecimal,
    ) : StudentRecord {
        override val graduationType: GraduationType get() = GraduationType.GED

        init {
            require(averageScore >= BigDecimal.ZERO) { "검정고시 평균점수는 음수일 수 없음: $averageScore" }
        }
    }
}

/** 한 학년의 미인정(무단) 출결 횟수 */
data class AttendanceRecord(
    /** 미인정 결석 일수 */
    val absenceDays: Int,
    /** 미인정 지각 횟수 */
    val latenessCount: Int,
    /** 미인정 조퇴 횟수 */
    val earlyLeaveCount: Int,
    /** 미인정 결과(缺課) 횟수 */
    val classAbsenceCount: Int,
) {
    init {
        require(absenceDays >= 0 && latenessCount >= 0 && earlyLeaveCount >= 0 && classAbsenceCount >= 0) {
            "출결 횟수는 음수일 수 없음: $this"
        }
    }

    /** 결석으로 환산하기 전의 지각·조퇴·결과 횟수 합 */
    val latenessTotal: Int = latenessCount + earlyLeaveCount + classAbsenceCount
}
