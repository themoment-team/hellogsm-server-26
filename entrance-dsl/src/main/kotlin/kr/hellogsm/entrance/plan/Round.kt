package kr.hellogsm.entrance.plan

import java.math.BigDecimal

/** N차 전형의 한 차수 */
data class Round(
    val code: String,
    val name: String,
    val selection: SelectionRule,
    /** 학과 구분 없는 총원제(All-cut) 선발 여부 */
    val allCut: Boolean,
    val score: ScoreComposition,
    val absentPolicy: AbsentPolicy,
    /** 동점자 처리 기준, 우선순위 순 */
    val tiebreakers: List<Tiebreaker>,
) {
    /** 이 차수의 점수 구성요소 목록 */
    val scoreComponents: List<ScoreComponent> =
        when (val s = score) {
            is ScoreComposition.Sum -> s.components
            is ScoreComposition.Weighted -> s.parts.map(ScoreComposition.Weighted.Part::component)
        }

    /** 이 차수에서 선언된 수동 입력 점수 목록 */
    val manualScores: List<ScoreComponent.ManualScore> =
        scoreComponents.filterIsInstance<ScoreComponent.ManualScore>()
}

sealed interface SelectionRule {
    /** 전형별 모집정원 × 배수 이내 선발 (예: 1차 전형 1.3배수) */
    data class Multiplier(val value: BigDecimal) : SelectionRule

    /** 전형별 모집정원만큼 선발 */
    data object Capacity : SelectionRule
}

/** 전형 불참(미응시)자 처리 */
enum class AbsentPolicy {
    /** 전형 대상에서 제외 */
    EXCLUDE,
}

sealed interface ScoreComposition {
    /** 구성요소 단순 합산 (예: 1차 = 교과 + 출석 + 봉사 = 300점) */
    data class Sum(val components: List<ScoreComponent>) : ScoreComposition

    /** 가중 합산 (예: 2차 = 1차 성적 50% + 역량검사 30% + 심층면접 20% = 100점) */
    data class Weighted(
        val maxScore: BigDecimal,
        val parts: List<Part>,
    ) : ScoreComposition {
        data class Part(
            val component: ScoreComponent,
            /** 반영 비율(%) — 한 composition의 합은 100이어야 한다 */
            val weightPercent: BigDecimal,
            /** 반영 전 이 만점으로 정규화 (예: 1차 300점 → 100점) */
            val normalizeTo: BigDecimal? = null,
        )
    }
}

sealed interface ScoreComponent {
    /** 교과 성적 (일반교과 + 예체능) */
    data object SubjectScore : ScoreComponent

    /** 출석 성적 */
    data object AttendanceScore : ScoreComponent

    /** 봉사활동 성적 */
    data object VolunteerScore : ScoreComponent

    /** 이전 차수의 총점 */
    data class RoundScore(val roundCode: String) : ScoreComponent

    /** 운영자가 수동 입력하는 점수 (예: 역량검사, 심층면접) */
    data class ManualScore(
        val code: String,
        val name: String,
        val maxScore: BigDecimal,
    ) : ScoreComponent
}

sealed interface Tiebreaker {
    /** 예체능 제외 일반교과 성적이 우수한 자 */
    data object GeneralSubjectScore : Tiebreaker

    /** 지정된 학기 순서대로 학기 성적 비교 (예: 3-1 → 2-2 → 2-1 → 1-2) */
    data class SemesterScores(val order: List<SemesterRef>) : Tiebreaker

    /** 비교과(출석 + 봉사) 성적이 우수한 자 */
    data object NonSubjectScore : Tiebreaker

    /** 수동 입력 점수가 우수한 자 (해당 차수에 선언된 ManualScore 코드) */
    data class ManualScore(val code: String) : Tiebreaker
}
