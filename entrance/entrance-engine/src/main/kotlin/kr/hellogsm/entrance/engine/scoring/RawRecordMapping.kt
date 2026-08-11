package kr.hellogsm.entrance.engine.scoring

import kr.hellogsm.entrance.plan.Achievement
import kr.hellogsm.entrance.plan.SemesterRef

/**
 * 원서 시스템의 원시 성적 표현(성취도 환산점수 배열·학년별 출결 배열)을 [StudentRecord] 구성요소로
 * 옮기는 공용 변환.
 *
 * `entrance-batch`(JPA 엔티티)와 `entrance-lambda`(요청 DTO)는 입력을 담는 타입만 다를 뿐 해석
 * 규칙은 같아야 한다 — 한쪽만 고치면 배치(최종 채점)와 Lambda(모의 계산)의 결과가 조용히
 * 갈라지므로, 규칙 자체는 이곳 한 곳에만 둔다. 각 모듈의 `StudentRecordMapper` 는 자기 소스
 * 타입에서 값을 꺼내는 일만 한다.
 *
 * go-hellogsm-score-calculator(scoring parity 기준 커밋 d7b65b4)의 입력 해석을 그대로 재현한다.
 */
object RawRecordMapping {

    /**
     * 학기별 일반교과 성취도. 성적이 비어 있는 학기는 키를 넣지 않는다.
     *
     * plan이 직접 채점하지 않는 학기(예: 1-1)도 그대로 넘긴다 — 결측 학기 대체
     * ([kr.hellogsm.entrance.plan.MissingSemesterStrategy])의 원본으로 쓰일 수 있어, 여기서 미리
     * 걸러내면 [ScoringEngine]이 plan에 선언된 대체 규칙을 적용할 방법이 없어진다.
     */
    fun generalAchievements(rawBySemester: Map<SemesterRef, List<Int>?>): Map<SemesterRef, List<Achievement>> =
        rawBySemester.mapValues { (_, raw) -> achievements(raw) }.filterValues { it.isNotEmpty() }

    /** 성취도 환산점수 → 성취도. **0(미수강)은 제외**하고 5→A … 1→E 로 변환한다. */
    fun achievements(raw: List<Int>?): List<Achievement> =
        raw.orEmpty().filter { it != 0 }.map(::achievement)

    /**
     * 학년별 미인정 출결. [absentDays]는 학년별 결석 일수(3개), [attendanceDays]는 학년별
     * 지각·조퇴·결과(학년당 3개, 총 9개)다.
     *
     * go는 결석 합 + (지각·조퇴·결과 합 ÷ 3)을 환산 결석으로 쓰는데, 엔진 [AttendanceRecord]는
     * 학년 합산 후 `latenessPerAbsenceDay`(=3)로 나누므로 셋을 latenessCount 하나로 합쳐도 점수가
     * 같다. 학년 3개를 모두 채워 결측 학년 기본점 경로를 타지 않게 한다.
     */
    fun attendanceByYear(absentDays: List<Int>?, attendanceDays: List<Int>?): Map<Int, AttendanceRecord> {
        val absent = absentDays.orEmpty()
        val lateness = attendanceDays.orEmpty()
        return (1..YEARS).associateWith { year ->
            val base = (year - 1) * LATENESS_KINDS
            AttendanceRecord(
                absenceDays = absent.getOrElse(year - 1) { 0 },
                latenessCount = (0 until LATENESS_KINDS).sumOf { lateness.getOrElse(base + it) { 0 } },
                earlyLeaveCount = 0,
                classAbsenceCount = 0,
            )
        }
    }

    /** 학년별 연간 봉사활동 시간 — 1학년부터 순서대로 담긴 배열을 학년 → 시간 맵으로 옮긴다. */
    fun volunteerHoursByYear(hoursByYear: List<Int>?): Map<Int, Int> =
        hoursByYear.orEmpty().mapIndexed { index, hours -> (index + 1) to hours }.toMap()

    private fun achievement(convertedScore: Int): Achievement = when (convertedScore) {
        5 -> Achievement.A
        4 -> Achievement.B
        3 -> Achievement.C
        2 -> Achievement.D
        1 -> Achievement.E
        else -> throw IllegalArgumentException("유효하지 않은 성취도 환산점수: $convertedScore (허용 1..5, 0=미수강)")
    }

    /** 중학교 학년 수 */
    private const val YEARS = 3

    /** 학년당 `attendanceDays` 칸 수 — 지각·조퇴·결과 */
    private const val LATENESS_KINDS = 3
}
