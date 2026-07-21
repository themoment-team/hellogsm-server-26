package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.engine.assignment.AssignmentEngine
import kr.hellogsm.entrance.engine.assignment.FinalCandidate
import kr.hellogsm.entrance.plans.plan2026
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 최종 합격자 발표 이후의 흐름을 이어서 검증한다:
 * 학과 배정 → 미등록·합격취소 발생 → 예비 합격자 승격 → (여전히 미달이면) 추가모집.
 *
 * 각 단계의 단위 규칙은 AssignmentEngineTest·AdditionalRecruitmentTest가 고정하고,
 * 이 테스트는 단계 간 입출력이 실제로 맞물리는지를 본다.
 */
class PostAdmissionFlowTest {

    private val evaluation = EvaluationEngine(plan2026)
    private val assignment = AssignmentEngine(plan2026)

    private fun candidate(id: String, score: String) = FinalCandidate(
        applicant = testApplicant(id, "GEN", total = "290.000", general = "100.000"),
        finalScore = BigDecimal(score),
        majorChoices = listOf("SW", "IOT", "AI"),
    )

    private fun ranked(prefix: String, count: Int, from: String = "100.000") = (1..count).map { i ->
        candidate(
            id = "$prefix$i",
            score = BigDecimal(from).subtract(BigDecimal("0.001").multiply(BigDecimal(i - 1))).toPlainString(),
        )
    }

    @Test
    fun `포기자 자리를 예비합격자로 채우고 남은 자리는 추가모집으로 채운다`() {
        // 1) 최종 합격자 72명 학과 배정
        val assigned = assignment.assign(ranked("c", count = 72))
        assertEquals(72, assigned.assignments.size)

        // 2) 예비 합격자 선정 — 일반전형 불합격자 중 고득점순, 정원의 3%(72 × 3% = 2.16 → 2명)
        val rejected = ranked("r", count = 10, from = "50.000")
        val waitlisted = assignment.waitlist(rejected)
        assertEquals(listOf("r1", "r2"), waitlisted)

        // 3) 5명이 미등록·합격취소 → 예비 합격자 2명을 승격해도 3자리가 남는다
        val withdrawn = listOf("c1", "c2", "c40", "c41", "c60")
        val promoted = rejected.filter { it.applicant.id in waitlisted }
        val afterWaitlist = assignment.reassign(assigned, withdrawn, promoted)

        assertEquals(listOf("r1", "r2"), afterWaitlist.promoted.map { it.applicantId })
        assertEquals(69, afterWaitlist.assignments.size)
        assertEquals(3, afterWaitlist.unfilled.totalWithinCapacity)

        // 4) 미달분 3자리를 추가모집으로 선발 — 일반전형만, 1차 환산점수만으로
        val newApplicants = rankedApplicants("n", "GEN", count = 8)
        val additional = evaluation.evaluateAdditionalRecruitment(
            newApplicants,
            openSeats = afterWaitlist.unfilled.totalWithinCapacity,
        )
        assertEquals(listOf("n1", "n2", "n3"), additional.passed.map { it.applicantId })

        // 5) 추가모집 합격자까지 승격 후보에 넣어 재배정하면 정원이 다시 찬다.
        //    재배정은 (최초 배정 + 전체 포기자 + 전체 후보)로 한 번에 계산하므로,
        //    단계를 나눠 부르든 합쳐 부르든 결과가 같다.
        val additionalCandidates = additional.passed.map { entry ->
            FinalCandidate(
                applicant = newApplicants.first { it.id == entry.applicantId },
                finalScore = entry.roundScore!!,
                majorChoices = listOf("SW", "IOT", "AI"),
            )
        }
        val filled = assignment.reassign(assigned, withdrawn, promoted + additionalCandidates)

        assertEquals(72, filled.assignments.size)
        assertEquals(0, filled.unfilled.totalWithinCapacity)
        assertEquals(
            listOf("n1", "n2", "n3", "r1", "r2"),
            filled.promoted.map { it.applicantId }.sorted(),
        )
        // 승격자는 포기자가 비운 학과(SW 2, IOT 2, AI 1)에만 들어간다
        assertEquals(
            mapOf("SW" to 2, "IOT" to 2, "AI" to 1),
            filled.promoted.groupingBy { it.majorCode }.eachCount(),
        )
    }
}
