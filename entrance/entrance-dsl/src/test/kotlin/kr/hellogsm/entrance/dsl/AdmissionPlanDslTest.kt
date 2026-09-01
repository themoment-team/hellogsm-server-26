package kr.hellogsm.entrance.dsl

import kr.hellogsm.entrance.plan.Achievement.A
import kr.hellogsm.entrance.plan.Achievement.B
import kr.hellogsm.entrance.plan.Achievement.C
import kr.hellogsm.entrance.plan.Achievement.D
import kr.hellogsm.entrance.plan.Achievement.E
import kr.hellogsm.entrance.plan.AdmissionPlan
import kr.hellogsm.entrance.plan.GraduationType.CANDIDATE
import kr.hellogsm.entrance.plan.MissingSemesterStrategy.SAME_YEAR_OTHER_SEMESTER
import kr.hellogsm.entrance.plan.Quota
import kr.hellogsm.entrance.plan.ScoreComponent
import kr.hellogsm.entrance.plan.SelectionRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** 검증 테스트가 공유하는 최소 유효 plan 조각들 */
internal fun GradingBuilder.standardGrading() {
    rounding(intermediateScale = 5, resultScale = 3)
    transcript(CANDIDATE) {
        generalSubjects(max = 180) {
            achievement(A to 5, B to 4, C to 3, D to 2, E to 1)
            semester(1, 2, points = 18)
            semester(2, 1, points = 45)
            semester(2, 2, points = 45)
            semester(3, 1, points = 72)
            missingSemester(SAME_YEAR_OTHER_SEMESTER)
        }
        artsSubjects(max = 60) { achievement(A to 5, B to 4, C to 3) }
        attendance(max = 30) {
            latenessPerAbsenceDay = 3
            deductionPerAbsenceDay = 3
            zeroFromAbsenceDays = 10
            missingYearDefault = 5
        }
        volunteer(maxPerYear = 10) {
            step(minHours = 7, points = 10)
            floor(2)
            missingYearDefault = 2
        }
    }
}

internal fun RoundsBuilder.standardFirstRound() {
    round("FIRST", "1차 전형") {
        selectByMultiplier(1.3)
        sumScore(subjectScore, attendanceScore, volunteerScore)
        tiebreak { byGeneralSubjectScore() }
    }
}

internal fun buildPlan(
    majorsBlock: MajorsBuilder.() -> Unit = { major("SW", "소프트웨어개발과", capacity = 10) },
    screeningsBlock: ScreeningsBuilder.() -> Unit = { regular("GEN", "일반전형") },
    roundsBlock: RoundsBuilder.() -> Unit = { standardFirstRound() },
    assignmentBlock: MajorAssignmentBuilder.() -> Unit = { choiceCount = 1 },
    waitlistBlock: (WaitlistBuilder.() -> Unit)? = null,
    additionalBlock: (AdditionalRecruitmentBuilder.() -> Unit)? = null,
): AdmissionPlan = admissionPlan(year = 2026) {
    majors(majorsBlock)
    screenings(screeningsBlock)
    grading { standardGrading() }
    rounds(roundsBlock)
    majorAssignment(assignmentBlock)
    waitlistBlock?.let { waitlist(it) }
    additionalBlock?.let { additionalRecruitment(it) }
}

class AdmissionPlanDslTest {

    @Test
    fun `최소 구성의 plan을 빌드할 수 있다`() {
        val plan = buildPlan()

        assertEquals(2026, plan.year)
        assertEquals(10, plan.totalCapacity)
        assertEquals(Quota.Remainder, plan.screening("GEN").quota)
        assertEquals(10, plan.resolvedQuota("GEN"))
    }

    @Test
    fun `Remainder 정원은 고정 정원을 제외한 나머지로 해석된다`() {
        val plan = buildPlan(screeningsBlock = {
            regular("GEN", "일반전형")
            regular("SPE", "특별전형") { quota(3) }
        })

        assertEquals(7, plan.resolvedQuota("GEN"))
        assertEquals(3, plan.resolvedQuota("SPE"))
    }

    @Test
    fun `정원 외 전형의 정원은 총정원 계산에 포함되지 않는다`() {
        val plan = buildPlan(screeningsBlock = {
            regular("GEN", "일반전형")
            extra("EXT", "정원외") { quota(2) }
        })

        assertEquals(10, plan.resolvedQuota("GEN"))
        assertEquals(2, plan.resolvedQuota("EXT"))
        assertTrue(plan.screening("GEN").withinCapacity)
        assertTrue(!plan.screening("EXT").withinCapacity)
    }

    @Test
    fun `round의 점수 구성요소와 수동 점수가 모델에 반영된다`() {
        val plan = buildPlan(roundsBlock = {
            standardFirstRound()
            round("SECOND", "2차 전형") {
                selectByCapacity()
                val interview = manualScore("INTERVIEW", "심층면접", max = 100)
                weightedScore(max = 100) {
                    part(roundScore("FIRST"), weightPercent = 70, normalizeTo = 100)
                    part(interview, weightPercent = 30)
                }
                tiebreak { byManualScore("INTERVIEW") }
            }
        })

        val second = plan.round("SECOND")
        assertIs<SelectionRule.Capacity>(second.selection)
        assertEquals(1, second.manualScores.size)
        assertEquals("INTERVIEW", second.manualScores.single().code)
        assertTrue(second.scoreComponents.any { it is ScoreComponent.RoundScore && it.roundCode == "FIRST" })
    }

    @Test
    fun `grading 블록 없이 빌드하면 실패한다`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            admissionPlan(year = 2026) {
                majors { major("SW", "소프트웨어개발과", capacity = 10) }
                screenings { regular("GEN", "일반전형") }
                rounds { standardFirstRound() }
                majorAssignment { choiceCount = 1 }
            }
        }
        assertTrue("grading" in exception.message.orEmpty())
    }

    @Test
    fun `정원 외 전형은 quota 선언이 없으면 실패한다`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            buildPlan(screeningsBlock = {
                regular("GEN", "일반전형")
                extra("EXT", "정원외") { }
            })
        }
        assertTrue("quota" in exception.message.orEmpty())
    }
}
