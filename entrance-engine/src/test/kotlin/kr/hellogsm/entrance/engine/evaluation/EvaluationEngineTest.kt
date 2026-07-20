package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.plan.GraduationType.GED
import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plans.plan2026
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EvaluationEngineTest {

    private val engine = EvaluationEngine(plan2026)

    // ── 1차 전형: 선발 인원과 배수 ─────────────────────────────────

    @Test
    fun `1차 일반전형은 정원 64명의 1_3배수를 올림한 84명을 선발한다`() {
        val applicants = rankedApplicants("g", "GEN", count = 100)
        val result = engine.evaluate("FIRST", applicants)

        assertEquals(84, result.passed.size)
        assertEquals(RoundOutcome.PASSED, result.entry("g84").outcome)
        assertEquals(RoundOutcome.REJECTED, result.entry("g85").outcome)
        assertEquals("GEN", result.entry("g85").appliedScreening)
    }

    @Test
    fun `1차 특별전형은 8명의 1_3배수를 올림한 11명을 선발한다`() {
        val applicants = rankedApplicants("s", "SPE", count = 11) // 전원 11명 이내
        val result = engine.evaluate("FIRST", applicants)

        assertEquals(11, result.passed.size)
        assertTrue(result.passed.all { it.appliedScreening == "SPE" })
    }

    @Test
    fun `정원 외 전형은 배수 없이 정원만큼 선발한다`() {
        val veterans = rankedApplicants("v", "EXT_VETERANS", count = 3)
        val result = engine.evaluate("FIRST", veterans)

        // 국가보훈 정원 2명 — 1.3배수(2.6→3)가 아니라 2명만 정원 외로 선발
        val passedVeterans = result.passed.filter { it.appliedScreening == "EXT_VETERANS" }
        assertEquals(2, passedVeterans.size)
    }

    // ── 편입(cascade) ─────────────────────────────────────────────

    @Test
    fun `정원 외 탈락자는 사회통합전형에 편입되어 전형된다`() {
        // 보훈 3명(정원 2) — 최하위 v3가 SPE 풀로 편입, SPE 풀(v3 포함 3명)은 11명 이내라 전원 선발
        val applicants = rankedApplicants("v", "EXT_VETERANS", count = 3) +
            rankedApplicants("s", "SPE", count = 2, startTotal = BigDecimal("250.000"))
        val result = engine.evaluate("FIRST", applicants)

        val v3 = result.entry("v3")
        assertEquals(RoundOutcome.PASSED, v3.outcome)
        assertEquals("SPE", v3.appliedScreening)
        assertEquals(listOf("EXT_VETERANS", "SPE"), v3.screeningPath)
    }

    @Test
    fun `특별전형 탈락자는 일반전형에 편입되어 전형된다`() {
        // SPE 13명(선발 11) — 최하위 2명이 GEN 풀로 편입되어 GEN에서 함께 전형
        val applicants = rankedApplicants("s", "SPE", count = 13) +
            rankedApplicants("g", "GEN", count = 5, startTotal = BigDecimal("200.000"))
        val result = engine.evaluate("FIRST", applicants)

        val s12 = result.entry("s12")
        val s13 = result.entry("s13")
        assertEquals("GEN", s12.appliedScreening)
        assertEquals(RoundOutcome.PASSED, s12.outcome)
        assertEquals(listOf("SPE", "GEN"), s13.screeningPath)
    }

    @Test
    fun `편입자도 일반전형 선발 경쟁에서 밀리면 탈락한다`() {
        // GEN 84명 만석 + SPE 탈락 편입자 2명이 GEN 하위 점수 → 편입자 탈락
        val applicants = rankedApplicants("g", "GEN", count = 84) + // 299.999 ~ 299.916
            rankedApplicants("s", "SPE", count = 13, startTotal = BigDecimal("100.000"))
        val result = engine.evaluate("FIRST", applicants)

        val s13 = result.entry("s13")
        assertEquals(RoundOutcome.REJECTED, s13.outcome)
        assertEquals("GEN", s13.appliedScreening) // 마지막으로 전형된 전형
        assertEquals(listOf("SPE", "GEN"), s13.screeningPath)
    }

    // ── 2차 전형: 합성 점수·미충원 이월·불참 ───────────────────────

    private fun secondRoundApplicant(
        id: String,
        screening: String,
        first: String,
        competency: String,
        interview: String,
    ) = testApplicant(
        id = id,
        screening = screening,
        total = first,
        manualScores = mapOf("COMPETENCY" to competency, "INTERVIEW" to interview),
        previousRoundScores = mapOf("FIRST" to first),
    )

    @Test
    fun `2차 점수는 1차 정규화 50 역량 30 면접 20으로 합성한다`() {
        // (240 ÷ 3) × 0.5 + 90 × 0.3 + 80 × 0.2 = 40 + 27 + 16 = 83
        val result = engine.evaluate(
            "SECOND",
            listOf(secondRoundApplicant("a", "GEN", first = "240.000", competency = "90", interview = "80")),
        )

        assertEquals(BigDecimal("83.000"), result.entry("a").roundScore)
    }

    @Test
    fun `2차 합성 점수는 중간 scale 5, 결과 scale 3으로 반올림한다`() {
        // 250 ÷ 3 = 83.33333… → 83.33333 → × 0.5 = 41.666665 / + 23.1 + 17.6 = 82.366665 → 82.367
        val result = engine.evaluate(
            "SECOND",
            listOf(secondRoundApplicant("a", "GEN", first = "250.000", competency = "77", interview = "88")),
        )

        assertEquals(BigDecimal("82.367"), result.entry("a").roundScore)
    }

    @Test
    fun `2차 미응시자는 전형 대상에서 제외한다`() {
        val absent = testApplicant(
            id = "absent",
            screening = "GEN",
            total = "299.000",
            manualScores = mapOf("COMPETENCY" to "100"), // 면접 미응시
            previousRoundScores = mapOf("FIRST" to "299.000"),
        )
        val present = secondRoundApplicant("present", "GEN", first = "150.000", competency = "50", interview = "50")

        val result = engine.evaluate("SECOND", listOf(absent, present))

        val entry = result.entry("absent")
        assertEquals(RoundOutcome.ABSENT, entry.outcome)
        assertEquals(null, entry.roundScore)
        assertEquals(RoundOutcome.PASSED, result.entry("present").outcome)
    }

    @Test
    fun `2차 특별전형 미충원분은 일반전형 선발 인원에 이월된다`() {
        // SPE 응시 5명(정원 8) → 미충원 3명이 GEN(64)에 이월되어 67명 선발
        val spe = (1..5).map { secondRoundApplicant("s$it", "SPE", "250.000", "9$it", "90") }
        val gen = (1..70).map { i ->
            secondRoundApplicant("g$i", "GEN", first = "240.000", competency = "${50 + i * 0.5}", interview = "70")
        }
        val result = engine.evaluate("SECOND", spe + gen)

        val passedGen = result.passed.filter { it.appliedScreening == "GEN" }
        assertEquals(67, passedGen.size)
        assertEquals(5, result.passed.count { it.appliedScreening == "SPE" })
    }

    @Test
    fun `1차 배수 선발에서는 미충원 이월이 없다`() {
        // SPE 5명(11명 이내 전원 선발)이어도 GEN 선발 인원은 84명 그대로
        val applicants = rankedApplicants("s", "SPE", count = 5) +
            rankedApplicants("g", "GEN", count = 90, startTotal = BigDecimal("250.000"))
        val result = engine.evaluate("FIRST", applicants)

        assertEquals(84, result.passed.count { it.appliedScreening == "GEN" })
    }

    // ── 동점자 처리 ───────────────────────────────────────────────

    @Test
    fun `총점 동점이면 일반교과 성적이 우수한 자가 앞선다`() {
        // GEN 정원 84명을 동점 응시자 85명으로 채워 경계 강제
        val filler = rankedApplicants("f", "GEN", count = 83)
        val tieHigh = testApplicant("tieHigh", "GEN", total = "100.000", general = "90.000")
        val tieLow = testApplicant("tieLow", "GEN", total = "100.000", general = "89.000")

        val result = engine.evaluate("FIRST", filler + tieLow + tieHigh)

        assertEquals(RoundOutcome.PASSED, result.entry("tieHigh").outcome)
        assertEquals(RoundOutcome.REJECTED, result.entry("tieLow").outcome)
    }

    @Test
    fun `일반교과도 같으면 3-1, 2-2, 2-1, 1-2 순서로 학기 성적을 비교한다`() {
        val filler = rankedApplicants("f", "GEN", count = 83)
        val base = mapOf(SemesterRef(3, 1) to "70.000", SemesterRef(2, 2) to "40.000")
        val tieHigh = testApplicant(
            "tieHigh", "GEN", total = "100.000", general = "90.000",
            semesters = base + (SemesterRef(2, 1) to "41.000"),
        )
        val tieLow = testApplicant(
            "tieLow", "GEN", total = "100.000", general = "90.000",
            semesters = base + (SemesterRef(2, 1) to "40.000"),
        )

        val result = engine.evaluate("FIRST", filler + tieLow + tieHigh)

        assertEquals(RoundOutcome.PASSED, result.entry("tieHigh").outcome)
        assertEquals(RoundOutcome.REJECTED, result.entry("tieLow").outcome)
    }

    @Test
    fun `검정고시 지원자는 교과·학기 동점자 기준에서 내신 지원자보다 뒤로 밀린다`() {
        val filler = rankedApplicants("f", "GEN", count = 83)
        val transcript = testApplicant("transcript", "GEN", total = "100.000", general = "0.000")
        val ged = testApplicant("ged", "GEN", total = "100.000", graduationType = GED)

        val result = engine.evaluate("FIRST", filler + ged + transcript)

        assertEquals(RoundOutcome.PASSED, result.entry("transcript").outcome)
        assertEquals(RoundOutcome.REJECTED, result.entry("ged").outcome)
    }

    @Test
    fun `2차 동점이면 역량검사, 심층면접 순으로 우수한 자가 앞선다`() {
        // 합성점수 동점: (300÷3)×0.5 + c×0.3 + i×0.2 조합
        val spe = (1..7).map { secondRoundApplicant("s$it", "SPE", "299.000", "99", "99") } // 정원 8 중 7석 선점
        val tieHigh = secondRoundApplicant("tieHigh", "SPE", first = "300.000", competency = "80", interview = "70")
        val tieLow = secondRoundApplicant("tieLow", "SPE", first = "300.000", competency = "76", interview = "76")
        // tieHigh = 50+24+14 = 88 / tieLow = 50+22.8+15.2 = 88 — 역량검사 80 > 76

        val result = engine.evaluate("SECOND", spe + tieLow + tieHigh)

        // 남은 SPE 1석을 두고 동점 — 역량검사가 높은 tieHigh가 SPE, tieLow는 GEN으로 편입
        assertEquals("SPE", result.entry("tieHigh").appliedScreening)
        assertEquals("GEN", result.entry("tieLow").appliedScreening)
    }

    @Test
    fun `선발 경계의 완전 동점은 위원회 결정이 필요하므로 실패한다`() {
        val identical = { id: String ->
            testApplicant(id, "EXT_VETERANS", total = "200.000", general = "120.000")
        }
        val error = assertFailsWith<UnresolvedTieException> {
            engine.evaluate("FIRST", listOf(identical("t1"), identical("t2"), identical("t3")))
        }
        assertEquals(listOf("t1", "t2", "t3"), error.applicantIds.sorted())
    }

    @Test
    fun `경계에 걸리지 않은 완전 동점은 전형을 막지 않는다`() {
        val applicants = listOf(
            testApplicant("t1", "GEN", total = "200.000"),
            testApplicant("t2", "GEN", total = "200.000"),
        )
        val result = engine.evaluate("FIRST", applicants)

        assertEquals(2, result.passed.size)
    }

    // ── 정원 외 1차 합격선 플래그 ──────────────────────────────────

    @Test
    fun `정원 내 합격자 최저점 미만인 정원 외 합격자에게 확인 플래그를 남긴다`() {
        val gen = rankedApplicants("g", "GEN", count = 10, startTotal = BigDecimal("250.000"))
        val veteranLow = testApplicant("vLow", "EXT_VETERANS", total = "100.000")
        val veteranHigh = testApplicant("vHigh", "EXT_VETERANS", total = "260.000")

        val result = engine.evaluate("FIRST", gen + veteranLow + veteranHigh)

        assertTrue(result.entry("vLow").belowFirstRoundCutline)
        assertFalse(result.entry("vHigh").belowFirstRoundCutline)
        assertFalse(result.entry("g1").belowFirstRoundCutline)
    }

    // ── 입력 검증 ─────────────────────────────────────────────────

    @Test
    fun `중복 식별자, 없는 전형, 범위 밖 수동 점수는 입력 오류다`() {
        val a = testApplicant("dup", "GEN", total = "200.000")
        assertFailsWith<EvaluationException> { engine.evaluate("FIRST", listOf(a, a)) }

        assertFailsWith<EvaluationException> {
            engine.evaluate("FIRST", listOf(testApplicant("x", "NOPE", total = "200.000")))
        }

        assertFailsWith<EvaluationException> {
            engine.evaluate(
                "SECOND",
                listOf(
                    testApplicant(
                        "x", "GEN", total = "200.000",
                        manualScores = mapOf("COMPETENCY" to "101", "INTERVIEW" to "50"),
                        previousRoundScores = mapOf("FIRST" to "200.000"),
                    ),
                ),
            )
        }
    }

    @Test
    fun `2차 평가에 1차 점수가 없으면 입력 오류다`() {
        assertFailsWith<EvaluationException> {
            engine.evaluate(
                "SECOND",
                listOf(
                    testApplicant(
                        "x", "GEN", total = "200.000",
                        manualScores = mapOf("COMPETENCY" to "50", "INTERVIEW" to "50"),
                    ),
                ),
            )
        }
    }
}
