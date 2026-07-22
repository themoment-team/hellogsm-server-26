package kr.hellogsm.entrance.batch.report

import kr.hellogsm.entrance.batch.pipeline.LoadedApplicant
import org.springframework.stereotype.Component
import java.math.BigDecimal

/**
 * 재계산 총점 vs DB 저장 `documentEvaluationScore`(Lambda 산출값) 대조.
 *
 * 요강 vs 기존 Go 산출에는 이미 알려진 미세 차이(학기 몫 scale-5 중간 반올림 ~±0.001,
 * 검정고시 float64 오차)가 있으므로 hard-fail 하지 않고, **허용오차 밖 불일치만** 감사 리포트로
 * 올린다. 재계산 결과가 정답이며 저장값을 덮어쓰지 않는다.
 */
@Component
class ScoreReconciliation {

    fun report(loaded: List<LoadedApplicant>, tolerance: BigDecimal = DEFAULT_TOLERANCE): List<Mismatch> {
        val mismatches = loaded.mapNotNull { la ->
            val stored = la.testResult.documentEvaluationScore ?: return@mapNotNull null
            val diff = (la.recomputedTotal - stored).abs()
            if (diff > tolerance) Mismatch(la.applicant.id, la.recomputedTotal, stored, diff) else null
        }

        if (mismatches.isEmpty()) {
            println("[대조] 재계산 총점 vs 저장 documentEvaluationScore — 허용오차($tolerance) 밖 불일치 없음")
        } else {
            println("[대조] 허용오차($tolerance) 밖 불일치 ${mismatches.size}건 (검토 필요):")
            mismatches.forEach { println("  - 원서 ${it.applicantId}: 재계산=${it.recomputed}, 저장=${it.stored}, 차=${it.diff}") }
        }
        return mismatches
    }

    data class Mismatch(
        val applicantId: String,
        val recomputed: BigDecimal,
        val stored: BigDecimal,
        val diff: BigDecimal,
    )

    companion object {
        val DEFAULT_TOLERANCE: BigDecimal = BigDecimal("0.001")
    }
}
