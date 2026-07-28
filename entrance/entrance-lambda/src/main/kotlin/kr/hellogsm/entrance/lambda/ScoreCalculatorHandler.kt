package kr.hellogsm.entrance.lambda

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kr.hellogsm.entrance.engine.scoring.ScoringEngine
import kr.hellogsm.entrance.plans.plan

private const val API_KEY_HEADER = "x-hg-api-key"
private const val API_KEY_ENV_VAR = "X_HG_INTERNAL_API_KEY"

/**
 * 모의 성적 계산 API. API Gateway 프록시 통합 뒤에서 동작하며, `go-hellogsm-score-calculator`와
 * 요청/응답 JSON 계약·인증 헤더가 동일하다 — server는 `SCORE_CALCULATOR_SERVICE_URL`만 바꾸면
 * 코드 변경 없이 이 람다를 대신 호출할 수 있다.
 *
 * Spring을 쓰지 않는다: server 다운 시에도 동작해야 하는 가용성 요건상 콜드 스타트를
 * 최소화해야 하기 때문이다(PLAN.md 9절).
 */
class ScoreCalculatorHandler @JvmOverloads constructor(
    private val expectedApiKey: String =
        System.getenv(API_KEY_ENV_VAR) ?: error("$API_KEY_ENV_VAR 환경변수가 설정되지 않았습니다"),
) : RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val scoringEngine = ScoringEngine(plan)
    private val resultScale = plan.grading.rounding.resultScale

    override fun handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent {
        val headerApiKey = input.headers.orEmpty().entries
            .firstOrNull { it.key.equals(API_KEY_HEADER, ignoreCase = true) }
            ?.value

        if (headerApiKey != expectedApiKey) {
            context.logger.log("Authorization failed: missing or invalid $API_KEY_HEADER")
            return errorResponse(401, "UNAUTHORIZED", "허가되지 않은 클라이언트 요청")
        }

        val body = input.body
        if (body.isNullOrEmpty()) {
            return errorResponse(400, "EMPTY_BODY", "Request body is empty")
        }

        val request = try {
            objectMapper.readValue(body, ScoreCalculatorRequest::class.java)
        } catch (e: JsonProcessingException) {
            context.logger.log("JSON unmarshal error: ${e.message}")
            return errorResponse(400, "INVALID_JSON", "Invalid JSON format: ${e.message}")
        }

        return try {
            val studentRecord = StudentRecordMapper.toStudentRecord(request)
            val breakdown = scoringEngine.score(studentRecord)
            val response = ScoreResponseMapper.toResponse(breakdown, resultScale)

            APIGatewayProxyResponseEvent()
                .withStatusCode(200)
                .withHeaders(mapOf("Content-Type" to "application/json"))
                .withBody(objectMapper.writeValueAsString(response))
        } catch (e: IllegalArgumentException) {
            // StudentRecordMapper 는 IllegalArgumentException, ScoringEngine 은 그 하위 타입인
            // ScoringException 을 던진다 — 둘 다 "요청 입력이 유효하지 않음"으로 취급한다.
            context.logger.log("Validation error: ${e.message}")
            errorResponse(400, "VALIDATION_ERROR", e.message ?: "요청 값이 유효하지 않음")
        }
    }

    private fun errorResponse(statusCode: Int, code: String, message: String): APIGatewayProxyResponseEvent {
        val body = objectMapper.writeValueAsString(ErrorResponse(error = "Validation Error", message = message, code = code))
        return APIGatewayProxyResponseEvent()
            .withStatusCode(statusCode)
            .withHeaders(mapOf("Content-Type" to "application/json"))
            .withBody(body)
    }
}
