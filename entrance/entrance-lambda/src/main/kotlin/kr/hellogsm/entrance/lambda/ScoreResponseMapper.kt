package kr.hellogsm.entrance.lambda

import kr.hellogsm.entrance.engine.scoring.ScoreBreakdown
import kr.hellogsm.entrance.plan.SemesterRef
import java.math.BigDecimal

/**
 * `ScoreBreakdown`(엔진 산출 결과) → [ScoreCalculatorResponse](server 응답 계약) 변환.
 *
 * plan에 반영되지 않는 학기(졸업예정자의 3-2, 졸업자의 1-2)는 엔진 결과에 키 자체가 없는데,
 * 기존 Go 구현은 이 경우도 0.000으로 명시해 응답한다 — 그 계약을 그대로 유지한다.
 */
object ScoreResponseMapper {

    fun toResponse(breakdown: ScoreBreakdown, resultScale: Int): ScoreCalculatorResponse {
        val detail = breakdown.transcriptDetail
        if (detail == null) {
            return ScoreCalculatorResponse(
                attendanceScore = breakdown.attendanceScore,
                volunteerScore = breakdown.volunteerScore,
                totalScore = breakdown.totalScore,
            )
        }

        val zero = BigDecimal.ZERO.setScale(resultScale)
        fun semester(year: Int, semester: Int): BigDecimal =
            detail.semesterScores[SemesterRef(year, semester)] ?: zero

        return ScoreCalculatorResponse(
            generalSubjectsScore = detail.generalSubjectsScore,
            generalSubjectsScoreDetail = GeneralSubjectsScoreDetail(
                score1_2 = semester(1, 2),
                score2_1 = semester(2, 1),
                score2_2 = semester(2, 2),
                score3_1 = semester(3, 1),
                score3_2 = semester(3, 2),
            ),
            artsPhysicalSubjectsScore = detail.artsSubjectsScore,
            totalSubjectsScore = breakdown.subjectsScore,
            attendanceScore = breakdown.attendanceScore,
            volunteerScore = breakdown.volunteerScore,
            totalScore = breakdown.totalScore,
        )
    }
}
