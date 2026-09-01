package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.engine.scoring.ScoreBreakdown
import kr.hellogsm.entrance.plan.Round
import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plan.Tiebreaker
import java.math.BigDecimal

/**
 * plan의 동점자 처리 기준([Tiebreaker] 목록)을 지원자 비교자로 해석한다.
 *
 * 모든 기준은 "점수가 우수한(높은) 자" 우선이며, 값이 없는 지원자는 가장 낮게 취급한다.
 * 값 규칙 (기존 구현과 동일):
 * - 검정고시는 일반교과·학기별 성적이 없으므로(null) 해당 기준에서 항상 뒤로 밀린다.
 * - 내신 지원자의 반영 외 학기(예: 졸업자의 1-2)는 0점으로 취급한다 (null보다 앞).
 */
internal object Tiebreakers {

    /** 점수가 높은 순으로 정렬하는 비교자. 앞선 기준부터 차례로 적용한다. */
    fun comparatorFor(round: Round): Comparator<RoundApplicant> =
        round.tiebreakers
            .map(::single)
            .reduceOrNull(Comparator<RoundApplicant>::thenComparing)
            ?: Comparator { _, _ -> 0 }

    private fun single(tiebreaker: Tiebreaker): Comparator<RoundApplicant> = when (tiebreaker) {
        is Tiebreaker.GeneralSubjectScore ->
            descendingBy { it.breakdown.transcriptDetail?.generalSubjectsScore }

        is Tiebreaker.SemesterScores ->
            tiebreaker.order
                .map { semester -> descendingBy { it.breakdown.semesterScoreForTiebreak(semester) } }
                .reduce(Comparator<RoundApplicant>::thenComparing)

        is Tiebreaker.NonSubjectScore ->
            descendingBy { it.breakdown.nonSubjectsScore }

        is Tiebreaker.ManualScore ->
            descendingBy { it.manualScores[tiebreaker.code] }
    }

    /** 내신 지원자는 반영 외 학기를 0점으로, 검정고시는 null(최하위)로 취급 */
    private fun ScoreBreakdown.semesterScoreForTiebreak(semester: SemesterRef): BigDecimal? =
        transcriptDetail?.let { it.semesterScores[semester] ?: BigDecimal.ZERO }

    /** null을 최하위로 두고 값이 큰 쪽을 앞세우는 비교자 */
    private fun descendingBy(selector: (RoundApplicant) -> BigDecimal?): Comparator<RoundApplicant> =
        Comparator { a, b ->
            val x = selector(a)
            val y = selector(b)
            when {
                x == null && y == null -> 0
                x == null -> 1
                y == null -> -1
                else -> y.compareTo(x)
            }
        }
}
