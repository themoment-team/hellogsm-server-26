package kr.hellogsm.entrance.batch.config

import kr.hellogsm.entrance.engine.assignment.AssignmentEngine
import kr.hellogsm.entrance.engine.evaluation.EvaluationEngine
import kr.hellogsm.entrance.engine.scoring.ScoringEngine
import kr.hellogsm.entrance.plan.AdmissionPlan
import kr.hellogsm.entrance.plans.plan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 엔진 3종을 요강(현재 [plan])으로 구성해 빈으로 등록한다.
 * 연도별 plan 교체는 이 설정만 바꾸면 된다 — 엔진·잡 코드는 plan 을 주입받아 동작한다.
 */
@Configuration
class EngineConfig {

    @Bean
    fun admissionPlan(): AdmissionPlan = plan

    @Bean
    fun scoringEngine(plan: AdmissionPlan): ScoringEngine = ScoringEngine(plan)

    @Bean
    fun evaluationEngine(plan: AdmissionPlan): EvaluationEngine = EvaluationEngine(plan)

    @Bean
    fun assignmentEngine(plan: AdmissionPlan): AssignmentEngine = AssignmentEngine(plan)
}
