package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.engine.scoring.ScoreBreakdown
import java.math.BigDecimal

/**
 * 한 차수 전형에 응시하는 지원자.
 *
 * [screening]은 이 차수를 시작하는 시점의 전형이다 — 1차에서는 지원자가 원서에 쓴 지망 전형,
 * 2차에서는 1차 결과로 확정된 적용 전형([RoundEntry.appliedScreening])을 넣는다.
 */
data class RoundApplicant(
    /** 외부 식별자 (원서 id 등). 결과 매칭에만 쓰이며 순위에 영향을 주지 않는다 */
    val id: String,
    /** 이 차수 시작 시점의 전형 코드 */
    val screening: String,
    /** 성적 계산(scoring) 결과 — 차수 점수와 동점자 처리의 근거 */
    val breakdown: ScoreBreakdown,
    /** 응시한 수동 입력 점수 (역량검사·심층면접 등). 미응시 항목은 키를 넣지 않는다 */
    val manualScores: Map<String, BigDecimal> = emptyMap(),
    /** 이전 차수들의 확정 점수 (예: 2차 평가 시 1차 점수). key = 차수 코드 */
    val previousRoundScores: Map<String, BigDecimal> = emptyMap(),
)

/** 차수 전형 결과 */
data class RoundResult(
    val roundCode: String,
    val entries: List<RoundEntry>,
) {
    val passed: List<RoundEntry> = entries.filter { it.outcome == RoundOutcome.PASSED }

    fun entry(applicantId: String): RoundEntry =
        entries.firstOrNull { it.applicantId == applicantId }
            ?: error("결과에 없는 지원자: $applicantId")
}

/** 지원자 한 명의 차수 전형 결과와 산출 근거 */
data class RoundEntry(
    val applicantId: String,
    val outcome: RoundOutcome,
    /** 최종 적용 전형. 합격 시 실제 선발된 전형, 탈락 시 마지막으로 전형된 전형. 불참이면 null */
    val appliedScreening: String?,
    /** 이 차수의 환산 점수. 불참이면 null */
    val roundScore: BigDecimal?,
    /**
     * 전형 편입 이력 (감사 기록). 시작 전형부터 순서대로.
     * (예: [EXT_VETERANS, SPE, GEN] = 국가보훈 → 사회통합 편입 → 일반 편입)
     */
    val screeningPath: List<String>,
    /**
     * 정원 외 전형 합격자가 정원 내 전형 1차 합격선(합격자 최저점) 미만인지 여부.
     * 요강상 "1차 합격자 최저점 이내에 포함된 경우에 한하여" 정원 외로 전형하므로,
     * true면 입학전형위원회의 확인이 필요하다 (엔진이 자동으로 편입하지 않는다 — 기존 Go 구현도 미구현).
     */
    val belowFirstRoundCutline: Boolean = false,
)

enum class RoundOutcome {
    /** 선발 */
    PASSED,

    /** 탈락 (편입 포함 모든 전형에서 선발되지 못함) */
    REJECTED,

    /** 불참(미응시) — 전형 대상 제외 */
    ABSENT,
}

/** 전형 배치 입력이 잘못되어 평가를 진행할 수 없을 때의 오류 */
class EvaluationException(message: String) : IllegalArgumentException(message)

/**
 * 동점자 처리 기준으로도 순위가 갈리지 않는 완전 동점이 선발 경계에 걸린 경우.
 * 요강에 다음 기준이 없으므로 입학전형위원회 결정(수동 오버라이드)이 필요하다.
 */
class UnresolvedTieException(
    val roundCode: String,
    val screeningCode: String?,
    val applicantIds: List<String>,
) : IllegalStateException(
    "동점자 처리 기준으로도 순위가 갈리지 않음 (차수: $roundCode, 전형: ${screeningCode ?: "-"}, " +
        "지원자: $applicantIds) — 입학전형위원회 결정 필요",
)
