package kr.hellogsm.entrance.engine.scoring

import kr.hellogsm.entrance.plans.plan
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 기존 Go 계산기(go-hellogsm-score-calculator, 2026 시즌 코드) 대비 golden test.
 *
 * fixture는 tools/golden/generate_golden.py 가 Go의 big.Rat 산술을 포팅해 생성한다.
 * Phase 1 완료 기준(PLAN.md 8절)의 parity 검증이며, 요강과 Go가 갈리는 지점은
 * [specDivergenceCases]에 요강 기준 기대값으로 분리 고정되어 있다 (요강이 정답 — PLAN.md 7절 2항).
 */
class GoParityGoldenTest {

    private val engine = ScoringEngine(plan)

    @Test
    fun `2026 시즌 Go 계산기와 동일한 결과를 낸다`() {
        goParityGoldenCases.forEach(::assertCase)
    }

    @Test
    fun `학기 몫 중간 반올림이 Go와 갈리는 케이스는 요강 방식으로 산출한다`() {
        specDivergenceCases.forEach(::assertCase)
    }

    private fun assertCase(case: GoldenCase) {
        val breakdown = engine.score(case.record)
        val expected = case.expected

        assertEquals(BigDecimal(expected.total), breakdown.totalScore, "${case.name}: 총점")
        assertEquals(BigDecimal(expected.subjects), breakdown.subjectsScore, "${case.name}: 교과")
        assertEquals(BigDecimal(expected.attendance), breakdown.attendanceScore, "${case.name}: 출석")
        assertEquals(BigDecimal(expected.volunteer), breakdown.volunteerScore, "${case.name}: 봉사")

        val detail = breakdown.transcriptDetail
        expected.general?.let { assertEquals(BigDecimal(it), detail!!.generalSubjectsScore, "${case.name}: 일반교과") }
        expected.arts?.let { assertEquals(BigDecimal(it), detail!!.artsSubjectsScore, "${case.name}: 예체능") }
        expected.semesters?.forEach { (semester, score) ->
            assertEquals(BigDecimal(score), detail!!.semesterScores[semester], "${case.name}: $semester 학기")
        }
    }
}
