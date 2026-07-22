package kr.hellogsm.entrance.batch.pipeline

import kr.hellogsm.entrance.engine.assignment.AssignmentEngine
import kr.hellogsm.entrance.engine.assignment.AssignmentResult
import kr.hellogsm.entrance.engine.assignment.FinalCandidate
import kr.hellogsm.entrance.engine.evaluation.EvaluationEngine
import kr.hellogsm.entrance.engine.evaluation.RoundResult
import org.springframework.stereotype.Component

/**
 * 1차 평가 → 2차 평가 → 학과 배정을 엔진에 위임하는 오케스트레이션.
 * go-hellogsm 대비 golden(BatchParityGoldenTest)의 구동 순서와 동일하게, 2차는 1차 합격자에게
 * 적용 전형·1차 점수를 이어주고, 배정은 2차 합격자를 최종 후보로 만든다.
 */
@Component
class EvaluationPipeline(
    private val evaluationEngine: EvaluationEngine,
    private val assignmentEngine: AssignmentEngine,
) {

    fun evaluateFirst(loaded: List<LoadedApplicant>): RoundResult =
        evaluationEngine.evaluate("FIRST", loaded.map { it.applicant })

    fun evaluateSecond(loaded: List<LoadedApplicant>, first: RoundResult): RoundResult {
        val byId = loaded.associateBy { it.applicant.id }
        val inputs = first.passed.map { entry ->
            byId.getValue(entry.applicantId).applicant.copy(
                screening = entry.appliedScreening!!,
                previousRoundScores = mapOf("FIRST" to entry.roundScore!!),
            )
        }
        return evaluationEngine.evaluate("SECOND", inputs)
    }

    fun assign(loaded: List<LoadedApplicant>, second: RoundResult): AssignmentResult {
        val byId = loaded.associateBy { it.applicant.id }
        val finalists = second.passed.map { entry ->
            val la = byId.getValue(entry.applicantId)
            FinalCandidate(
                applicant = la.applicant.copy(screening = entry.appliedScreening!!),
                finalScore = entry.roundScore!!,
                majorChoices = la.choices,
            )
        }
        return assignmentEngine.assign(finalists)
    }
}
