package kr.hellogsm.entrance.batch.pipeline

import kr.hellogsm.entrance.batch.mapping.CodeMapping
import kr.hellogsm.entrance.engine.assignment.AssignmentEngine
import kr.hellogsm.entrance.engine.assignment.AssignmentResult
import kr.hellogsm.entrance.engine.assignment.FinalCandidate
import kr.hellogsm.entrance.engine.evaluation.EvaluationEngine
import kr.hellogsm.entrance.engine.evaluation.RoundApplicant
import kr.hellogsm.entrance.engine.evaluation.RoundResult
import org.springframework.stereotype.Component
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo

/**
 * 1차 평가 → 2차 평가 → 학과 배정을 엔진에 위임하는 오케스트레이션.
 * go-hellogsm 대비 golden(BatchParityGoldenTest)의 구동 순서와 동일하게, 2차는 1차 합격자에게
 * 적용 전형·1차 점수를 이어주고, 배정은 2차 합격자를 최종 후보로 만든다.
 *
 * 각 단계는 **앞 단계가 DB에 확정해 둔 결과**(`firstTestPassYn`·`appliedScreening`,
 * `secondTestPassYn`)에서 출발한다 — 앞 차수를 다시 전형하지 않으므로 발표 후의 수동 정정
 * (위원회 결정 등)이 그대로 이어지고, 잡을 순서대로 돌릴 때 같은 전형이 반복 실행되지 않는다.
 * 다음 차수 입력에 필요한 이전 차수 점수만 [EvaluationEngine.scoreOf]로 다시 산출한다.
 */
@Component
class EvaluationPipeline(
    private val evaluationEngine: EvaluationEngine,
    private val assignmentEngine: AssignmentEngine,
) {

    fun evaluateFirst(loaded: List<LoadedApplicant>): RoundResult =
        evaluationEngine.evaluate(FIRST, loaded.map(LoadedApplicant::applicant))

    fun evaluateSecond(loaded: List<LoadedApplicant>): RoundResult =
        evaluationEngine.evaluate(SECOND, secondRoundInputs(loaded))

    fun assign(loaded: List<LoadedApplicant>): AssignmentResult {
        val byId = loaded.associateBy { it.applicant.id }
        val finalists = secondRoundInputs(loaded)
            .filter { byId.getValue(it.id).testResult.secondTestPassYn == YesNo.YES }
            .map { applicant ->
                FinalCandidate(
                    applicant = applicant,
                    finalScore = requireNotNull(evaluationEngine.scoreOf(SECOND, applicant)) {
                        "2차 합격자인데 2차 점수를 산출할 수 없음(역량검사·심층면접 점수 없음): 원서 ${applicant.id}"
                    },
                    majorChoices = byId.getValue(applicant.id).choices,
                )
            }
        return assignmentEngine.assign(finalists)
    }

    /**
     * 저장된 1차 결과로 만든 2차 차수 입력 — 1차 합격자만, 확정된 적용 전형과 1차 점수를 실어서.
     */
    private fun secondRoundInputs(loaded: List<LoadedApplicant>): List<RoundApplicant> =
        loaded.filter { it.testResult.firstTestPassYn == YesNo.YES }.map { la ->
            val applied = requireNotNull(la.oneseo.appliedScreening) {
                "1차 합격자에 적용 전형이 기록되어 있지 않음(first-eval 선행 필요): 원서 ${la.applicant.id}"
            }
            val firstScore = requireNotNull(evaluationEngine.scoreOf(FIRST, la.applicant)) {
                "1차 합격자인데 1차 점수를 산출할 수 없음: 원서 ${la.applicant.id}"
            }
            la.applicant.copy(
                screening = CodeMapping.screeningCode(applied),
                previousRoundScores = mapOf(FIRST to firstScore),
            )
        }

    private companion object {
        const val FIRST = "FIRST"
        const val SECOND = "SECOND"
    }
}
