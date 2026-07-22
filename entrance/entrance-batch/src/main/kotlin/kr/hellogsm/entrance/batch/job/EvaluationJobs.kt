package kr.hellogsm.entrance.batch.job

import kr.hellogsm.entrance.batch.mapping.CodeMapping
import kr.hellogsm.entrance.batch.pipeline.ApplicantLoader
import kr.hellogsm.entrance.batch.pipeline.EvaluationPipeline
import kr.hellogsm.entrance.batch.report.ScoreReconciliation
import kr.hellogsm.entrance.engine.evaluation.RoundOutcome
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo

/** 1차 전형(서류) 평가 → `firstTestPassYn` + 적용 전형(`appliedScreening`) 기록. */
@Component
class FirstEvaluationJob(
    private val loader: ApplicantLoader,
    private val pipeline: EvaluationPipeline,
    private val reconciliation: ScoreReconciliation,
) : BatchJob {

    override val name = "first-eval"

    @Transactional
    override fun run(dryRun: Boolean, options: Map<String, String>) {
        val loaded = loader.load()
        reconciliation.report(loaded)

        val byId = loaded.associateBy { it.applicant.id }
        val first = pipeline.evaluateFirst(loaded)

        var passed = 0
        first.entries.forEach { entry ->
            val la = byId.getValue(entry.applicantId)
            val pass = if (entry.outcome == RoundOutcome.PASSED) YesNo.YES else YesNo.NO
            if (pass == YesNo.YES) passed++
            if (!dryRun) {
                la.testResult.decideFirstTestResult(pass)
                entry.appliedScreening?.let { la.oneseo.applyScreening(CodeMapping.toScreening(it)) }
            }
        }
        println("[first-eval] 대상 ${loaded.size}명 · 1차 합격 $passed 명${dryRunSuffix(dryRun)}")
    }
}

/** 2차 전형(역량검사·심층면접) 평가 → `secondTestPassYn` 기록. */
@Component
class SecondEvaluationJob(
    private val loader: ApplicantLoader,
    private val pipeline: EvaluationPipeline,
) : BatchJob {

    override val name = "second-eval"

    @Transactional
    override fun run(dryRun: Boolean, options: Map<String, String>) {
        val loaded = loader.load()
        val byId = loaded.associateBy { it.applicant.id }

        val first = pipeline.evaluateFirst(loaded)
        val second = pipeline.evaluateSecond(loaded, first)

        var passed = 0
        second.entries.forEach { entry ->
            val la = byId.getValue(entry.applicantId)
            val pass = if (entry.outcome == RoundOutcome.PASSED) YesNo.YES else YesNo.NO
            if (pass == YesNo.YES) passed++
            if (!dryRun) la.testResult.decideSecondTestResult(pass)
        }
        println("[second-eval] 2차 응시 ${second.entries.size}명 · 2차 합격 $passed 명${dryRunSuffix(dryRun)}")
    }
}

/** 학과 배정 → `decidedMajor` + `passYn` 기록(합격자 YES/그 외 NO). */
@Component
class AssignmentJob(
    private val loader: ApplicantLoader,
    private val pipeline: EvaluationPipeline,
) : BatchJob {

    override val name = "assign"

    @Transactional
    override fun run(dryRun: Boolean, options: Map<String, String>) {
        val loaded = loader.load()

        val first = pipeline.evaluateFirst(loaded)
        val second = pipeline.evaluateSecond(loaded, first)
        val assignment = pipeline.assign(loaded, second)

        val assignedById = assignment.assignments.associateBy { it.applicantId }
        loaded.forEach { la ->
            val assigned = assignedById[la.applicant.id]
            if (!dryRun) {
                if (assigned != null) {
                    la.oneseo.decideAdmission(CodeMapping.toMajor(assigned.majorCode), YesNo.YES)
                } else {
                    la.oneseo.decideAdmission(null, YesNo.NO)
                }
            }
        }
        println("[assign] 최종 합격·배정 ${assignment.assignments.size}명${dryRunSuffix(dryRun)}")
    }
}

private fun dryRunSuffix(dryRun: Boolean): String = if (dryRun) " (dry-run, 미저장)" else " · 저장 완료"
