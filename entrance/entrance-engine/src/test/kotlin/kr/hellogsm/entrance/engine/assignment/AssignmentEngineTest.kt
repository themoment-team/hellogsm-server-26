package kr.hellogsm.entrance.engine.assignment

import kr.hellogsm.entrance.engine.evaluation.EvaluationException
import kr.hellogsm.entrance.engine.evaluation.UnresolvedTieException
import kr.hellogsm.entrance.engine.evaluation.testApplicant
import kr.hellogsm.entrance.plans.plan
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AssignmentEngineTest {

    private val engine = AssignmentEngine(plan)

    /** 최종 점수가 [score]인 배정 대상자. 동점자 기준은 일반교과 성적으로 구분 */
    private fun candidate(
        id: String,
        screening: String = "GEN",
        score: String,
        choices: List<String> = listOf("SW", "IOT", "AI"),
        general: String = "100.000",
    ) = FinalCandidate(
        applicant = testApplicant(id, screening, total = "290.000", general = general),
        finalScore = BigDecimal(score),
        majorChoices = choices,
    )

    /** 최종 점수가 100점부터 0.001씩 낮아지는 대상자 [count]명 */
    private fun rankedCandidates(prefix: String, count: Int, screening: String = "GEN"): List<FinalCandidate> =
        (1..count).map { i ->
            candidate(
                id = "$prefix$i",
                screening = screening,
                score = BigDecimal("100.000").subtract(BigDecimal("0.001").multiply(BigDecimal(i - 1))).toPlainString(),
            )
        }

    @Test
    fun `성적 상위순으로 1지망부터 배정하고 정원 초과 시 다음 지망으로 넘어간다`() {
        // 72명 전원 [SW, IOT, AI] 지망 → 상위 36명 SW(1지망), 다음 18명 IOT(2지망), 나머지 18명 AI(3지망)
        val result = engine.assign(rankedCandidates("c", count = 72))

        assertEquals(36, result.byMajor("SW").size)
        assertEquals(18, result.byMajor("IOT").size)
        assertEquals(18, result.byMajor("AI").size)

        assertEquals("SW", result.assignments[35].majorCode)
        assertEquals(1, result.assignments[35].choiceRank)
        assertEquals("IOT", result.assignments[36].majorCode)
        assertEquals(2, result.assignments[36].choiceRank)
        assertEquals("AI", result.assignments[54].majorCode)
        assertEquals(3, result.assignments[54].choiceRank)
    }

    @Test
    fun `지망이 다르면 후순위 성적이어도 원하는 학과에 배정된다`() {
        val candidates = listOf(
            candidate("top", score = "99.000", choices = listOf("SW", "IOT", "AI")),
            candidate("mid", score = "98.000", choices = listOf("IOT", "SW", "AI")),
            candidate("low", score = "97.000", choices = listOf("AI", "SW", "IOT")),
        )
        val result = engine.assign(candidates)

        assertEquals("SW", result.assignments.first { it.applicantId == "top" }.majorCode)
        assertEquals("IOT", result.assignments.first { it.applicantId == "mid" }.majorCode)
        assertEquals("AI", result.assignments.first { it.applicantId == "low" }.majorCode)
    }

    @Test
    fun `정원 외 합격자는 학과 정원과 별도로 학과당 최대 2명까지 배정된다`() {
        // 정원 내 72명이 SW부터 가득 채워도, 정원 외 3명은 별도 풀에서 배정
        val normal = rankedCandidates("n", count = 72)
        val extras = listOf(
            candidate("e1", screening = "EXT_VETERANS", score = "50.000"),
            candidate("e2", screening = "EXT_VETERANS", score = "49.000"),
            candidate("e3", screening = "EXT_SPECIAL", score = "48.000"),
        )
        val result = engine.assign(normal + extras)

        // 정원 외 SW 상한 2명 → e1, e2는 SW(1지망), e3는 IOT(2지망)
        assertEquals("SW", result.assignments.first { it.applicantId == "e1" }.majorCode)
        assertEquals("SW", result.assignments.first { it.applicantId == "e2" }.majorCode)
        assertEquals("IOT", result.assignments.first { it.applicantId == "e3" }.majorCode)
        // 정원 내 배정은 영향 없음
        assertEquals(36, result.byMajor("SW").count { it.screening != "EXT_VETERANS" && it.screening != "EXT_SPECIAL" })
    }

    @Test
    fun `배정 순서의 완전 동점은 위원회 결정이 필요하므로 실패한다`() {
        val candidates = listOf(
            candidate("t1", score = "90.000"),
            candidate("t2", score = "90.000"),
        )
        val error = assertFailsWith<UnresolvedTieException> { engine.assign(candidates) }
        assertEquals(listOf("t1", "t2"), error.applicantIds.sorted())
    }

    @Test
    fun `지망 학과 수가 다르거나 중복이면 입력 오류다`() {
        assertFailsWith<EvaluationException> {
            engine.assign(listOf(candidate("x", score = "90.000", choices = listOf("SW", "IOT"))))
        }
        assertFailsWith<EvaluationException> {
            engine.assign(listOf(candidate("x", score = "90.000", choices = listOf("SW", "SW", "AI"))))
        }
    }

    @Test
    fun `정원 내 합격자가 총정원을 초과하면 입력 오류다`() {
        assertFailsWith<EvaluationException> {
            engine.assign(rankedCandidates("c", count = 73))
        }
    }

    // ── 예비 합격자 ───────────────────────────────────────────────

    @Test
    fun `예비합격은 일반전형 불합격자 중 고득점순으로 정원의 3퍼센트(내림)까지다`() {
        // 72명 × 3% = 2.16 → 2명
        val rejected = listOf(
            candidate("g1", screening = "GEN", score = "80.000"),
            candidate("g2", screening = "GEN", score = "85.000"),
            candidate("g3", screening = "GEN", score = "70.000"),
            candidate("s1", screening = "SPE", score = "90.000"), // 일반전형이 아니므로 제외
        )
        assertEquals(listOf("g2", "g1"), engine.waitlist(rejected))
    }

    // ── 중도포기 재배정 ───────────────────────────────────────────

    /** 정원 내 72명 배정 결과 (상위 36명 SW, 다음 18명 IOT, 나머지 18명 AI) */
    private fun fullAssignment() = engine.assign(rankedCandidates("c", count = 72))

    @Test
    fun `중도포기자가 비운 자리에 예비합격자를 성적순으로 배정한다`() {
        val previous = fullAssignment()
        // c1은 SW, c40은 IOT에 배정되어 있다
        val waitlist = listOf(
            candidate("w1", score = "50.000"),
            candidate("w2", score = "49.000"),
        )
        val result = engine.reassign(previous, listOf("c1", "c40"), waitlist)

        assertEquals("SW", result.promoted.first { it.applicantId == "w1" }.majorCode)
        assertEquals("IOT", result.promoted.first { it.applicantId == "w2" }.majorCode)
        assertEquals(2, result.promoted.size)
        assertEquals(72, result.assignments.size)
        assertEquals(0, result.unfilled.totalWithinCapacity)
    }

    @Test
    fun `기존 합격자의 배정 학과는 재배정에서 바뀌지 않는다`() {
        val previous = fullAssignment()
        val result = engine.reassign(previous, listOf("c1"), listOf(candidate("w1", score = "50.000")))

        val unchanged = previous.assignments.filterNot { it.applicantId == "c1" }
        assertEquals(unchanged, result.assignments.filterNot { it.applicantId == "w1" })
    }

    @Test
    fun `중도포기자가 없으면 아무도 추가 배정되지 않는다`() {
        val result = engine.reassign(fullAssignment(), emptyList(), listOf(candidate("w1", score = "50.000")))

        assertEquals(emptyList(), result.promoted)
        assertEquals(0, result.unfilled.totalWithinCapacity)
    }

    @Test
    fun `예비합격자가 부족하면 남은 빈자리를 보고한다`() {
        val result = engine.reassign(fullAssignment(), listOf("c1", "c2", "c40"), emptyList())

        assertEquals(mapOf("SW" to 2, "IOT" to 1), result.unfilled.withinCapacity)
        assertEquals(3, result.unfilled.totalWithinCapacity)
        assertEquals(69, result.assignments.size)
    }

    @Test
    fun `정원 외 포기자의 자리는 정원 외 예비합격자만 채운다`() {
        val extras = listOf(
            candidate("e1", screening = "EXT_VETERANS", score = "50.000"),
            candidate("e2", screening = "EXT_SPECIAL", score = "49.000"),
        )
        val previous = engine.assign(rankedCandidates("c", count = 72) + extras)

        // e1(SW, 정원 외)이 포기 — 정원 내 예비합격자는 이 자리를 쓸 수 없다
        val normalOnly = engine.reassign(previous, listOf("e1"), listOf(candidate("w1", score = "40.000")))
        assertEquals(emptyList(), normalOnly.promoted)
        assertEquals(mapOf("SW" to 1), normalOnly.unfilled.extra)

        // 같은 자리를 정원 외 예비합격자는 채울 수 있다
        val extraCandidate = candidate("w2", screening = "EXT_VETERANS", score = "40.000")
        val extraFill = engine.reassign(previous, listOf("e1"), listOf(extraCandidate))
        assertEquals("SW", extraFill.promoted.single().majorCode)
    }

    @Test
    fun `정원 내 포기자가 생겨도 정원 외 자리는 열리지 않는다`() {
        // go-hellogsm은 정원 내 포기자만큼 정원 외 자리도 열어 학과당 2명 상한을 넘길 수 있다.
        // 요강상 두 상한은 독립이므로 엔진은 열지 않는다.
        val extras = listOf(
            candidate("e1", screening = "EXT_VETERANS", score = "50.000"),
            candidate("e2", screening = "EXT_VETERANS", score = "49.000"),
        )
        val previous = engine.assign(rankedCandidates("c", count = 72) + extras)
        assertEquals(2, previous.byMajor("SW").count { it.screening == "EXT_VETERANS" })

        val result = engine.reassign(
            previous,
            listOf("c1"), // 정원 내 SW 포기자
            listOf(candidate("w1", screening = "EXT_SPECIAL", score = "40.000")),
        )

        assertEquals(emptyList(), result.promoted)
        assertEquals(mapOf("SW" to 1), result.unfilled.withinCapacity)
        assertEquals(emptyMap(), result.unfilled.extra)
    }

    @Test
    fun `배정되지 않은 포기자나 이미 배정된 추가 대상은 입력 오류다`() {
        val previous = fullAssignment()

        assertFailsWith<EvaluationException> {
            engine.reassign(previous, listOf("nobody"), emptyList())
        }
        assertFailsWith<EvaluationException> {
            engine.reassign(previous, listOf("c1"), listOf(candidate("c2", score = "50.000")))
        }
    }
}
