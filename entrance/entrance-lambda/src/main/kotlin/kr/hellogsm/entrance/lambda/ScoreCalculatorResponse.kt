package kr.hellogsm.entrance.lambda

import com.fasterxml.jackson.annotation.JsonInclude
import java.math.BigDecimal

/**
 * server의 `CalculatedScoreResDto`와 JSON 형태가 동일해야 한다.
 * 검정고시(GED)는 [generalSubjectsScore]·[generalSubjectsScoreDetail]·[artsPhysicalSubjectsScore]·
 * [totalSubjectsScore]를 null로 두어 응답에서 생략한다 (기존 Go 구현과 동일한 계약).
 */
data class ScoreCalculatorResponse(
    @param:JsonInclude(JsonInclude.Include.NON_NULL) val generalSubjectsScore: BigDecimal? = null,
    @param:JsonInclude(JsonInclude.Include.NON_NULL) val generalSubjectsScoreDetail: GeneralSubjectsScoreDetail? = null,
    @param:JsonInclude(JsonInclude.Include.NON_NULL) val artsPhysicalSubjectsScore: BigDecimal? = null,
    @param:JsonInclude(JsonInclude.Include.NON_NULL) val totalSubjectsScore: BigDecimal? = null,
    val attendanceScore: BigDecimal,
    val volunteerScore: BigDecimal,
    val totalScore: BigDecimal,
)

/** 학기 중 plan에 반영되지 않는 학기(졸업예정자의 3-2, 졸업자의 1-2)는 0.000으로 채운다 — 기존 계약 유지 */
data class GeneralSubjectsScoreDetail(
    val score1_2: BigDecimal,
    val score2_1: BigDecimal,
    val score2_2: BigDecimal,
    val score3_1: BigDecimal,
    val score3_2: BigDecimal,
)

data class ErrorResponse(
    val error: String,
    val message: String,
    val code: String,
)
