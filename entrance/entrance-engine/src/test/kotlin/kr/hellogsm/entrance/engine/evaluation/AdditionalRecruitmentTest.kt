package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plans.plan2026
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 추가모집(요강 8-바) 테스트.
 *
 * 기존 go-hellogsm에 대응 배치가 없어 parity 검증 대상이 아니며, 기대값은 요강 조항
 * "일반전형으로 추가 모집" · "1차 전형 환산점수에 의한 성적 상위자로 선발"에서 직접 도출했다.
 */
class AdditionalRecruitmentTest {

    private val engine = EvaluationEngine(plan2026)

    @Test
    fun `1차 환산점수 상위순으로 빈자리 수만큼 선발한다`() {
        val applicants = rankedApplicants("a", "GEN", count = 5)
        val result = engine.evaluateAdditionalRecruitment(applicants, openSeats = 3)

        assertEquals(listOf("a1", "a2", "a3"), result.passed.map { it.applicantId })
        assertEquals(RoundOutcome.REJECTED, result.entry("a4").outcome)
        assertEquals("FIRST", result.roundCode)
    }

    @Test
    fun `선발 점수는 2차 합성이 아닌 1차 환산점수(교과+출석+봉사)다`() {
        // 역량검사·심층면접 점수가 있어도 무시하고 1차 총점만으로 줄 세운다
        val lowFirstHighManual = testApplicant(
            "lowFirst", "GEN", total = "200.000",
            manualScores = mapOf("COMPETENCY" to "100", "INTERVIEW" to "100"),
        )
        val highFirst = testApplicant("highFirst", "GEN", total = "250.000")

        val result = engine.evaluateAdditionalRecruitment(listOf(lowFirstHighManual, highFirst), openSeats = 1)

        assertEquals(listOf("highFirst"), result.passed.map { it.applicantId })
        assertEquals(BigDecimal("250.000"), result.entry("highFirst").roundScore)
    }

    @Test
    fun `빈자리보다 지원자가 적으면 전원 선발한다`() {
        val result = engine.evaluateAdditionalRecruitment(rankedApplicants("a", "GEN", count = 2), openSeats = 5)

        assertEquals(2, result.passed.size)
    }

    @Test
    fun `빈자리가 없으면 아무도 선발하지 않는다`() {
        val result = engine.evaluateAdditionalRecruitment(rankedApplicants("a", "GEN", count = 3), openSeats = 0)

        assertEquals(emptyList(), result.passed)
        assertTrue(result.entries.all { it.outcome == RoundOutcome.REJECTED })
    }

    @Test
    fun `동점자는 1차 전형 기준(일반교과, 학기 순, 비교과)으로 가른다`() {
        val high = testApplicant(
            "high", "GEN", total = "200.000", general = "120.000",
            semesters = mapOf(SemesterRef(3, 1) to "70.000"),
        )
        val low = testApplicant(
            "low", "GEN", total = "200.000", general = "120.000",
            semesters = mapOf(SemesterRef(3, 1) to "69.000"),
        )
        val result = engine.evaluateAdditionalRecruitment(listOf(low, high), openSeats = 1)

        assertEquals(listOf("high"), result.passed.map { it.applicantId })
    }

    @Test
    fun `선발 경계의 완전 동점은 위원회 결정이 필요하므로 실패한다`() {
        val tied = { id: String -> testApplicant(id, "GEN", total = "200.000", general = "120.000") }

        assertFailsWith<UnresolvedTieException> {
            engine.evaluateAdditionalRecruitment(listOf(tied("t1"), tied("t2")), openSeats = 1)
        }
    }

    @Test
    fun `일반전형이 아닌 지원자나 음수 선발 인원은 입력 오류다`() {
        assertFailsWith<EvaluationException> {
            engine.evaluateAdditionalRecruitment(rankedApplicants("s", "SPE", count = 1), openSeats = 1)
        }
        assertFailsWith<EvaluationException> {
            engine.evaluateAdditionalRecruitment(rankedApplicants("a", "GEN", count = 1), openSeats = -1)
        }
    }
}
