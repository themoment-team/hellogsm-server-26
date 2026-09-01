package kr.hellogsm.entrance.plans

import kr.hellogsm.entrance.plan.FormulaGrading
import kr.hellogsm.entrance.plan.GraduationType.CANDIDATE
import kr.hellogsm.entrance.plan.GraduationType.GED
import kr.hellogsm.entrance.plan.GraduationType.GRADUATE
import kr.hellogsm.entrance.plan.Quota
import kr.hellogsm.entrance.plan.ScoreComponent
import kr.hellogsm.entrance.plan.ScoreComposition
import kr.hellogsm.entrance.plan.SelectionRule
import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plan.Tiebreaker
import kr.hellogsm.entrance.plan.TranscriptGrading
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * 2026 요강(.reference/2026_entrance.pdf)의 수치가 plan에 정확히 옮겨졌는지 고정하는 테스트.
 * 요강 개정 없이 이 테스트가 깨진다면 plan이 잘못 수정된 것이다.
 */
class PlanTest {

    @Test
    fun `총 모집정원은 72명이다`() {
        assertEquals(72, plan.totalCapacity)
        assertEquals(36, plan.major("SW").capacity)
        assertEquals(18, plan.major("IOT").capacity)
        assertEquals(18, plan.major("AI").capacity)
    }

    @Test
    fun `일반전형 64명, 특별전형 8명이다`() {
        assertEquals(64, plan.resolvedQuota("GEN"))
        assertEquals(8, plan.resolvedQuota("SPE"))
    }

    @Test
    fun `특별전형 미충원분과 탈락자는 일반전형으로 이동한다`() {
        val spe = plan.screening("SPE")
        assertEquals("GEN", spe.unfilledGoesTo)
        assertEquals("GEN", spe.rejectedFallsTo)
    }

    @Test
    fun `사회통합전형 세부 유형은 5종이다`() {
        assertEquals(5, plan.screening("SPE").subTypes.size)
    }

    @Test
    fun `정원 외 전형 - 국가보훈 2명(3% 이내), 특례 1명(2% 이내)`() {
        val veterans = plan.screening("EXT_VETERANS")
        val special = plan.screening("EXT_SPECIAL")

        assertEquals(Quota.Fixed(2, BigDecimal(3)), veterans.quota)
        assertEquals(Quota.Fixed(1, BigDecimal(2)), special.quota)
        assertTrue(!veterans.withinCapacity)
        assertTrue(!special.withinCapacity)
    }

    @Test
    fun `정원 외 전형은 1차 합격선 이내일 때만 정원 외로 전형하고, 초과 시 사회통합전형에 편입된다`() {
        listOf("EXT_VETERANS", "EXT_SPECIAL").forEach { code ->
            val screening = plan.screening(code)
            assertTrue(screening.admitOnlyWithinFirstRoundCutline, code)
            assertEquals("SPE", screening.overflowFallsTo, code)
        }
    }

    @Test
    fun `졸업예정자 일반교과 학기 배점은 18-45-45-72다`() {
        val scheme = plan.grading.schemes.getValue(CANDIDATE) as TranscriptGrading
        val points = scheme.generalSubjects.semesterPoints.mapValues { it.value.toInt() }

        assertEquals(
            mapOf(
                SemesterRef(1, 2) to 18,
                SemesterRef(2, 1) to 45,
                SemesterRef(2, 2) to 45,
                SemesterRef(3, 1) to 72,
            ),
            points,
        )
        assertEquals(BigDecimal(300), scheme.maxScore)
    }

    @Test
    fun `졸업자 일반교과 학기 배점은 36-36-54-54다`() {
        val scheme = plan.grading.schemes.getValue(GRADUATE) as TranscriptGrading
        val points = scheme.generalSubjects.semesterPoints.mapValues { it.value.toInt() }

        assertEquals(
            mapOf(
                SemesterRef(2, 1) to 36,
                SemesterRef(2, 2) to 36,
                SemesterRef(3, 1) to 54,
                SemesterRef(3, 2) to 54,
            ),
            points,
        )
    }

    @Test
    fun `검정고시 교과는 (평균-50)÷50×240 환산식과 출석 30점 고정을 사용한다`() {
        val scheme = plan.grading.schemes.getValue(GED) as FormulaGrading

        assertEquals(BigDecimal(50), scheme.subjectFormula.minInput)
        assertEquals(BigDecimal(100), scheme.subjectFormula.maxInput)
        assertEquals(BigDecimal(240), scheme.subjectFormula.maxScore)
        assertEquals(BigDecimal(30), scheme.attendanceFixedScore)
    }

    @Test
    fun `검정고시 봉사는 (평균-40)÷60×30 환산식을 사용한다`() {
        val scheme = plan.grading.schemes.getValue(GED) as FormulaGrading

        assertEquals(BigDecimal(40), scheme.volunteerFormula.minInput)
        assertEquals(BigDecimal(100), scheme.volunteerFormula.maxInput)
        assertEquals(BigDecimal(30), scheme.volunteerFormula.maxScore)
    }

    @Test
    fun `봉사활동 계단 - 7시간 이상 10점부터 3시간 이하 2점까지`() {
        val scheme = plan.grading.schemes.getValue(CANDIDATE) as TranscriptGrading
        val steps = scheme.volunteer.steps.associate { it.minHours to it.points.toInt() }

        assertEquals(mapOf(7 to 10, 6 to 8, 5 to 6, 4 to 4), steps)
        assertEquals(BigDecimal(2), scheme.volunteer.floorScore)
        assertEquals(BigDecimal(30), scheme.volunteer.maxScore)
    }

    @Test
    fun `1차 전형은 1_3배수 총원제 선발이다`() {
        val first = plan.round("FIRST")

        val selection = assertIs<SelectionRule.Multiplier>(first.selection)
        assertEquals(0, BigDecimal("1.3").compareTo(selection.value))
        assertTrue(first.allCut)
        assertIs<ScoreComposition.Sum>(first.score)
    }

    @Test
    fun `1차 동점자 기준 - 일반교과, 학기 순(3-1, 2-2, 2-1, 1-2), 비교과`() {
        val tiebreakers = plan.round("FIRST").tiebreakers

        assertEquals(3, tiebreakers.size)
        assertIs<Tiebreaker.GeneralSubjectScore>(tiebreakers[0])
        val semesters = assertIs<Tiebreaker.SemesterScores>(tiebreakers[1])
        assertEquals(
            listOf(SemesterRef(3, 1), SemesterRef(2, 2), SemesterRef(2, 1), SemesterRef(1, 2)),
            semesters.order,
        )
        assertIs<Tiebreaker.NonSubjectScore>(tiebreakers[2])
    }

    @Test
    fun `2차 전형은 1차 50% + 역량검사 30% + 심층면접 20%다`() {
        val second = plan.round("SECOND")
        val score = assertIs<ScoreComposition.Weighted>(second.score)

        assertEquals(BigDecimal(100), score.maxScore)
        val weights = score.parts.associate { part ->
            when (val component = part.component) {
                is ScoreComponent.RoundScore -> component.roundCode
                is ScoreComponent.ManualScore -> component.code
                else -> error("예상치 못한 구성요소: $component")
            } to part.weightPercent.toInt()
        }
        assertEquals(mapOf("FIRST" to 50, "COMPETENCY" to 30, "INTERVIEW" to 20), weights)

        val firstPart = score.parts.single { it.component is ScoreComponent.RoundScore }
        assertEquals(BigDecimal(100), firstPart.normalizeTo)
    }

    @Test
    fun `최종 동점자 기준은 역량검사, 심층면접 순으로 시작한다`() {
        val tiebreakers = plan.round("SECOND").tiebreakers

        assertEquals(5, tiebreakers.size)
        assertEquals(Tiebreaker.ManualScore("COMPETENCY"), tiebreakers[0])
        assertEquals(Tiebreaker.ManualScore("INTERVIEW"), tiebreakers[1])
    }

    @Test
    fun `학과 배정 - 3지망 필수, 정원 외는 학과당 최대 2명`() {
        assertEquals(3, plan.majorAssignment.choiceCount)
        assertEquals(2, plan.majorAssignment.extraScreeningCapPerMajor)
    }

    @Test
    fun `예비합격은 일반전형에서 정원의 3% 범위다`() {
        val waitlist = plan.waitlist!!
        assertEquals(BigDecimal(3), waitlist.percentOfTotalCapacity)
        assertEquals("GEN", waitlist.fromScreening)
    }

    @Test
    fun `추가모집은 일반전형만, 1차 환산점수 기준이다`() {
        val additional = plan.additionalRecruitment!!
        assertEquals(listOf("GEN"), additional.screenings)
        assertEquals("FIRST", additional.basedOnRound)
    }
}
