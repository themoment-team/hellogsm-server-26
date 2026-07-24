package kr.hellogsm.entrance.lambda

import kr.hellogsm.entrance.engine.scoring.ScoringEngine
import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plans.plan2026
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * entrance-batch의 `StudentRecordMapperTest`와 동일한 원본 성적을 사용한다 — 모의 계산(이 람다)과
 * 최종 배치 계산이 같은 입력에 같은 점수를 내는지 교차 검증하는 목적이 크다.
 */
class StudentRecordMapperTest {

    private val scoring = ScoringEngine(plan2026)

    @Test
    fun `요청을 매핑해 채점하면 entrance-batch 매퍼와 동일한 점수가 나온다`() {
        val request = ScoreCalculatorRequest(
            achievement1_1 = listOf(3, 3, 3, 4, 5), // 1_2가 제출되어 대체가 필요 없으므로 반영되지 않아야 함
            achievement1_2 = listOf(3, 3, 3, 4, 5, 3, 1, 4, 2, 1),
            achievement2_1 = listOf(1, 3, 3, 1, 3, 5, 3),
            achievement2_2 = listOf(3, 4, 3, 1, 1, 5, 1),
            achievement3_1 = listOf(4, 5, 2, 4, 1, 3, 3, 5, 3, 1, 2),
            achievement3_2 = emptyList(),
            artsPhysicalAchievement = listOf(3, 3, 4, 5),
            absentDays = listOf(4, 1, 2),
            attendanceDays = listOf(0, 3, 2, 4, 1, 2, 4, 0, 1),
            volunteerTime = listOf(5, 2, 2),
            graduationType = "CANDIDATE",
        )

        val record = StudentRecordMapper.toStudentRecord(request)
        val score = scoring.score(record)

        assertEquals("156.212", score.totalScore.toPlainString(), "총점")
        assertEquals("146.212", score.subjectsScore.toPlainString(), "교과 총점")
        assertEquals("0.000", score.attendanceScore.toPlainString(), "출석 점수")
        assertEquals("10.000", score.volunteerScore.toPlainString(), "봉사 점수")
        assertEquals("101.212", score.transcriptDetail!!.generalSubjectsScore.toPlainString(), "일반교과")
        assertEquals("45.000", score.transcriptDetail!!.artsSubjectsScore.toPlainString(), "예체능")
    }

    @Test
    fun `1_2가 없고 1_1이 있으면 SAME_YEAR_OTHER_SEMESTER 전략으로 1_1을 1_2 대체 원본으로 쓴다`() {
        // 1_1을 직접 제출한 경우와, 처음부터 1_2 자리에 같은 성적을 제출한 경우가 같은 학기점수를 내야
        // "결측 학기 대체가 최종 채점 시점에 plan 선언대로 적용된다"는 것이 증명된다.
        val base = ScoreCalculatorRequest(
            achievement2_1 = listOf(3, 3, 3, 3, 3),
            achievement2_2 = listOf(4, 4, 4, 4, 4),
            achievement3_1 = listOf(3, 3, 3, 3, 3),
            artsPhysicalAchievement = listOf(3),
            graduationType = "CANDIDATE",
        )
        val viaFallback = base.copy(achievement1_1 = listOf(5, 5, 5, 5, 5))
        val submittedDirectly = base.copy(achievement1_2 = listOf(5, 5, 5, 5, 5))

        val scoreViaFallback = scoring.score(StudentRecordMapper.toStudentRecord(viaFallback))
        val scoreDirect = scoring.score(StudentRecordMapper.toStudentRecord(submittedDirectly))

        assertEquals(
            scoreDirect.transcriptDetail!!.semesterScores[SemesterRef(1, 2)],
            scoreViaFallback.transcriptDetail!!.semesterScores[SemesterRef(1, 2)],
            "1_1로 대체된 1_2 학기점수가 직접 제출한 것과 같아야 함",
        )

        val withoutAny1 = base // 1_1도 1_2도 없음 → UPPER_YEAR(2_2)로 대체되어 다른 값이 나와야 함
        val scoreWithoutAny1 = scoring.score(StudentRecordMapper.toStudentRecord(withoutAny1))
        assertNotEquals(
            scoreWithoutAny1.transcriptDetail!!.semesterScores[SemesterRef(1, 2)],
            scoreViaFallback.transcriptDetail!!.semesterScores[SemesterRef(1, 2)],
            "1_1이 있을 때와 없을 때(2_2로 대체) 학기점수가 달라야 함 — 우선순위가 실제로 적용됨을 확인",
        )
    }

    @Test
    fun `검정고시는 gedAvgScore 로 StudentRecord_Ged 를 만든다`() {
        val request = ScoreCalculatorRequest(
            gedAvgScore = BigDecimal("92.50"),
            graduationType = "GED",
        )

        val record = StudentRecordMapper.toStudentRecord(request)

        val score = scoring.score(record)
        assertEquals("GED", score.graduationType.name, "졸업 구분")
    }
}
