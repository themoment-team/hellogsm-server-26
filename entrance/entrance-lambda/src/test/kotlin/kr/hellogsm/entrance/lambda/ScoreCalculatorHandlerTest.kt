package kr.hellogsm.entrance.lambda

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreCalculatorHandlerTest {

    private val apiKey = "test-api-key"
    private val handler = ScoreCalculatorHandler(apiKey)
    private val context = FakeContext()
    private val objectMapper = jacksonObjectMapper()

    private fun request(body: String?, apiKeyHeader: String? = apiKey): APIGatewayProxyRequestEvent {
        val event = APIGatewayProxyRequestEvent().withBody(body)
        if (apiKeyHeader != null) event.withHeaders(mapOf("x-hg-api-key" to apiKeyHeader))
        return event
    }

    @Test
    fun `인증 헤더가 없으면 401을 반환한다`() {
        val response = handler.handleRequest(request(body = "{}", apiKeyHeader = null), context)
        assertEquals(401, response.statusCode)
    }

    @Test
    fun `인증 헤더가 틀리면 401을 반환한다`() {
        val response = handler.handleRequest(request(body = "{}", apiKeyHeader = "wrong-key"), context)
        assertEquals(401, response.statusCode)
    }

    @Test
    fun `body가 비어있으면 400 EMPTY_BODY를 반환한다`() {
        val response = handler.handleRequest(request(body = ""), context)
        assertEquals(400, response.statusCode)
        assertTrue(response.body.contains("EMPTY_BODY"))
    }

    @Test
    fun `JSON 형식이 아니면 400 INVALID_JSON을 반환한다`() {
        val response = handler.handleRequest(request(body = "not-json"), context)
        assertEquals(400, response.statusCode)
        assertTrue(response.body.contains("INVALID_JSON"))
    }

    @Test
    fun `졸업예정자 요청을 채점해 200과 총점을 반환한다`() {
        val body = objectMapper.writeValueAsString(
            ScoreCalculatorRequest(
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
            ),
        )

        val response = handler.handleRequest(request(body = body), context)

        assertEquals(200, response.statusCode)
        val parsed = objectMapper.readValue(response.body, ScoreCalculatorResponse::class.java)
        assertEquals("156.212", parsed.totalScore.toPlainString())
        assertEquals("0.000", parsed.generalSubjectsScoreDetail!!.score3_2.toPlainString(), "졸업예정자는 3-2 미반영이라 0.000")
    }

    @Test
    fun `검정고시 요청은 교과 상세 필드가 응답에서 생략된다`() {
        val body = objectMapper.writeValueAsString(
            ScoreCalculatorRequest(gedAvgScore = java.math.BigDecimal("92.50"), graduationType = "GED"),
        )

        val response = handler.handleRequest(request(body = body), context)

        assertEquals(200, response.statusCode)
        assertTrue(!response.body.contains("generalSubjectsScoreDetail"))
        assertTrue(!response.body.contains("artsPhysicalSubjectsScore"))
    }

    @Test
    fun `유효하지 않은 성취도 값은 400 VALIDATION_ERROR를 반환한다`() {
        val body = objectMapper.writeValueAsString(
            ScoreCalculatorRequest(achievement2_1 = listOf(9), graduationType = "CANDIDATE"),
        )

        val response = handler.handleRequest(request(body = body), context)

        assertEquals(400, response.statusCode)
        assertTrue(response.body.contains("VALIDATION_ERROR"))
    }
}
