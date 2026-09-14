package kr.hellogsm.entrance.plan

import kr.hellogsm.entrance.dsl.buildPlan
import kr.hellogsm.entrance.dsl.standardFirstRound
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlanValidatorTest {

    private fun assertPlanInvalid(errorFragment: String, block: () -> AdmissionPlan) {
        val exception = assertFailsWith<PlanValidationException> { block() }
        assertTrue(
            exception.errors.any { errorFragment in it },
            "오류 목록에 '$errorFragment'가 없음: ${exception.errors}",
        )
    }

    @Test
    fun `학과 코드가 중복되면 실패한다`() = assertPlanInvalid("학과 코드 중복") {
        buildPlan(majorsBlock = {
            major("SW", "소프트웨어개발과", capacity = 10)
            major("SW", "다른과", capacity = 5)
        })
    }

    @Test
    fun `학과 정원이 0 이하면 실패한다`() = assertPlanInvalid("정원은 양수여야 함") {
        buildPlan(majorsBlock = { major("SW", "소프트웨어개발과", capacity = 0) })
    }

    @Test
    fun `정원 내 고정 정원 합이 총정원을 초과하면 실패한다`() = assertPlanInvalid("총정원") {
        buildPlan(screeningsBlock = {
            regular("GEN", "일반전형") { quota(8) }
            regular("SPE", "특별전형") { quota(5) }
        })
    }

    @Test
    fun `Remainder 전형이 없으면 고정 정원 합이 총정원과 같아야 한다`() = assertPlanInvalid("같아야 함") {
        buildPlan(screeningsBlock = {
            regular("GEN", "일반전형") { quota(6) }
            regular("SPE", "특별전형") { quota(3) }
        })
    }

    @Test
    fun `Remainder 전형이 2개 이상이면 실패한다`() = assertPlanInvalid("최대 1개") {
        buildPlan(screeningsBlock = {
            regular("GEN", "일반전형")
            regular("GEN2", "일반전형2")
        })
    }

    @Test
    fun `정원 외 전형이 퍼센트 상한을 초과하면 실패한다`() = assertPlanInvalid("상한") {
        // 총정원 10명의 3% = 0.3명 → 내림 0명인데 2명을 선언
        buildPlan(screeningsBlock = {
            regular("GEN", "일반전형")
            extra("EXT", "정원외") { quota(2, capPercentOfTotal = 3) }
        })
    }

    @Test
    fun `fallback 대상 전형이 존재하지 않으면 실패한다`() = assertPlanInvalid("존재하지 않음") {
        buildPlan(screeningsBlock = {
            regular("GEN", "일반전형") { unfilledGoesTo("NOPE") }
        })
    }

    @Test
    fun `일반교과 학기 배점 합이 만점과 다르면 실패한다`() {
        val plan = buildPlan()
        val scheme = plan.grading.schemes.getValue(GraduationType.CANDIDATE) as TranscriptGrading
        val broken = scheme.copy(
            generalSubjects = scheme.generalSubjects.copy(
                semesterPoints = mapOf(SemesterRef(3, 1) to BigDecimal(100)),
            ),
        )

        assertPlanInvalid("학기별 배점 합") {
            plan.copy(grading = plan.grading.copy(schemes = mapOf(GraduationType.CANDIDATE to broken)))
        }
    }

    @Test
    fun `가중 점수의 반영 비율 합이 100%가 아니면 실패한다`() = assertPlanInvalid("100%가 아님") {
        buildPlan(roundsBlock = {
            standardFirstRound()
            round("SECOND", "2차 전형") {
                selectByCapacity()
                weightedScore(max = 100) {
                    part(roundScore("FIRST"), weightPercent = 50, normalizeTo = 100)
                    part(manualScore("INTERVIEW", "심층면접", max = 100), weightPercent = 30)
                }
            }
        })
    }

    @Test
    fun `이전 차수가 아닌 round 점수를 참조하면 실패한다`() = assertPlanInvalid("이전 차수가 아닌") {
        buildPlan(roundsBlock = {
            round("FIRST", "1차 전형") {
                selectByMultiplier(1.3)
                sumScore(roundScore("SECOND"), subjectScore)
            }
        })
    }

    @Test
    fun `동점자 기준이 선언되지 않은 수동 점수를 참조하면 실패한다`() = assertPlanInvalid("선언되지 않은 수동 점수") {
        buildPlan(roundsBlock = {
            round("FIRST", "1차 전형") {
                selectByMultiplier(1.3)
                sumScore(subjectScore, attendanceScore, volunteerScore)
                tiebreak { byManualScore("COMPETENCY") }
            }
        })
    }

    @Test
    fun `지망 학과 수가 학과 수를 초과하면 실패한다`() = assertPlanInvalid("지망 학과 수") {
        buildPlan(assignmentBlock = { choiceCount = 2 })
    }

    @Test
    fun `예비합격 대상 전형이 존재하지 않으면 실패한다`() = assertPlanInvalid("예비합격 대상 전형") {
        buildPlan(waitlistBlock = {
            percentOfTotal(3)
            from("NOPE")
        })
    }

    @Test
    fun `추가모집 기준 차수가 존재하지 않으면 실패한다`() = assertPlanInvalid("추가모집 기준 차수") {
        buildPlan(additionalBlock = {
            screening("GEN")
            basedOnRound("NOPE")
        })
    }
}
