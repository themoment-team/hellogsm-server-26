package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.engine.assignment.AssignmentEngine
import kr.hellogsm.entrance.engine.assignment.FinalCandidate
import kr.hellogsm.entrance.plans.plan2026
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 기존 배치(go-hellogsm, 2026 시즌 상수) 대비 golden test.
 *
 * fixture는 tools/golden/generate_batch_golden.py 가 배치 로직을 포팅해 생성한다.
 * 1차 평가 → 2차 평가 → 학과 배정을 엔진으로 이어 실행하며 각 단계의 결과를 전수 비교한다.
 * Phase 2 완료 기준(PLAN.md 8절)의 parity 검증 1단계다 (실 DB 배치 대비 검증은 별도).
 */
class BatchParityGoldenTest {

    private val evaluation = EvaluationEngine(plan2026)
    private val assignment = AssignmentEngine(plan2026)

    @Test
    fun `2026 시즌 배치와 동일하게 1차-2차-학과배정을 수행한다`() {
        loadBatchParityScenarios().forEach(::assertScenario)
    }

    private fun assertScenario(scenario: BatchScenario) {
        val name = scenario.name
        val byId = scenario.applicants.associateBy { it.applicant.id }

        // ── 1차 평가 ──
        val first = evaluation.evaluate("FIRST", scenario.applicants.map { it.applicant })

        scenario.expectedFirstApplied.forEach { (id, expectedApplied) ->
            val entry = first.entry(id)
            if (expectedApplied != null) {
                assertEquals(RoundOutcome.PASSED, entry.outcome, "$name/$id: 1차 결과")
                assertEquals(expectedApplied, entry.appliedScreening, "$name/$id: 1차 적용 전형")
            } else {
                assertEquals(RoundOutcome.REJECTED, entry.outcome, "$name/$id: 1차 결과")
            }
        }
        assertEquals(
            scenario.expectedFirstApplied.count { it.value != null },
            first.passed.size,
            "$name: 1차 합격자 수",
        )

        // ── 2차 평가 (1차 합격자만, 적용 전형과 1차 점수를 이어받음) ──
        val secondInputs = first.passed.map { entry ->
            byId.getValue(entry.applicantId).applicant.copy(
                screening = entry.appliedScreening!!,
                previousRoundScores = mapOf("FIRST" to entry.roundScore!!),
            )
        }
        val second = evaluation.evaluate("SECOND", secondInputs)

        assertEquals(
            scenario.expectedAbsent.sorted(),
            second.entries.filter { it.outcome == RoundOutcome.ABSENT }.map { it.applicantId }.sorted(),
            "$name: 2차 미응시자",
        )
        scenario.expectedSecondApplied.forEach { (id, expectedApplied) ->
            val entry = second.entry(id)
            if (expectedApplied != null) {
                assertEquals(RoundOutcome.PASSED, entry.outcome, "$name/$id: 2차 결과")
                assertEquals(expectedApplied, entry.appliedScreening, "$name/$id: 2차 적용 전형")
            } else {
                assertEquals(RoundOutcome.REJECTED, entry.outcome, "$name/$id: 2차 결과")
            }
        }
        scenario.expectedSecondScore.forEach { (id, expected) ->
            assertEquals(BigDecimal(expected), second.entry(id).roundScore, "$name/$id: 2차 합성 점수")
        }

        // ── 학과 배정 (최종 합격자) ──
        val finalists = second.passed.map { entry ->
            val golden = byId.getValue(entry.applicantId)
            FinalCandidate(
                applicant = golden.applicant.copy(screening = entry.appliedScreening!!),
                finalScore = entry.roundScore!!,
                majorChoices = golden.choices,
            )
        }
        val result = assignment.assign(finalists)

        assertEquals(
            scenario.expectedMajors,
            result.assignments.associate { it.applicantId to it.majorCode },
            "$name: 학과 배정",
        )
    }
}
