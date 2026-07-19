package kr.hellogsm.entrance.plan

import java.math.BigDecimal

/**
 * 전형 (예: 일반전형, 사회통합전형, 국가보훈대상자).
 *
 * 정원 내([withinCapacity] = true) 전형은 학과 총정원을 나눠 가지며,
 * 정원 외 전형은 총정원과 별개의 고정 정원(+% 상한)을 가진다.
 */
data class Screening(
    val code: String,
    val name: String,
    /** 정원 내 전형 여부. false면 정원 외 전형 */
    val withinCapacity: Boolean,
    val quota: Quota,
    /** 미충원 인원을 넘겨받아 추가 선발하는 전형 코드 (예: 특별전형 미충원분 → 일반전형) */
    val unfilledGoesTo: String? = null,
    /** 이 전형 탈락자가 같은 차수에서 편입되어 함께 전형되는 전형 코드 */
    val rejectedFallsTo: String? = null,
    /** 정원 외 전형 규칙: 1차(정원 내) 합격자 최저점 이내인 경우에만 정원 외로 전형 */
    val admitOnlyWithinFirstRoundCutline: Boolean = false,
    /** 정원 외 모집범위 초과 시 편입되는 전형 코드 (예: → 사회통합전형) */
    val overflowFallsTo: String? = null,
    /** 세부 자격 유형 (예: 기초생활수급자). 자격 심사 자체는 plan 밖의 운영 절차 */
    val subTypes: List<ScreeningSubType> = emptyList(),
)

data class ScreeningSubType(
    val code: String,
    val name: String,
)

sealed interface Quota {
    /**
     * 고정 인원. [capPercentOfTotal]이 있으면 "전체 모집정원의 n% 이내" 상한을 함께 검증한다.
     */
    data class Fixed(
        val count: Int,
        val capPercentOfTotal: BigDecimal? = null,
    ) : Quota

    /** 정원 내 다른 전형에 배정하고 남은 인원 전부 (예: 일반전형) */
    data object Remainder : Quota
}
