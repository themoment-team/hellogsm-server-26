package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.plan.AdmissionPlan
import kr.hellogsm.entrance.plan.Quota
import kr.hellogsm.entrance.plan.Round
import kr.hellogsm.entrance.plan.RoundingPolicy
import kr.hellogsm.entrance.plan.ScoreComponent
import kr.hellogsm.entrance.plan.ScoreComposition
import kr.hellogsm.entrance.plan.Screening
import kr.hellogsm.entrance.plan.SelectionRule
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 차수별 전형(선발·편입·탈락)을 수행하는 엔진.
 *
 * plan의 전형 규칙만 해석하는 순수 함수이며, 같은 입력이면 항상 같은 출력을 낸다.
 * 전형 처리 순서는 편입 관계(rejectedFallsTo / overflowFallsTo / unfilledGoesTo)의
 * 위상 순서를 따른다 (예: 2026 — 정원외 → 사회통합 → 일반).
 *
 * 엔진이 해석으로 확정한 규칙 (요강에 명시가 없어 기존 go-hellogsm 동작을 따름):
 * - 배수 선발 인원은 올림한다 (예: 64 × 1.3 = 83.2 → 84명).
 * - 정원 외 전형은 차수와 무관하게 배수를 적용하지 않고 정원만큼 선발한다 (요강 4.2의 "모집범위 내 전형").
 * - 미충원 이월(unfilledGoesTo)은 정원 선발 차수([SelectionRule.Capacity])에만 적용한다
 *   (배수 선발 차수의 정원은 선발 상한이지 모집 정원이 아니므로).
 * - 정원 외 전형의 "1차 합격자 최저점 이내" 조항은 자동 편입하지 않고
 *   [RoundEntry.belowFirstRoundCutline] 플래그로만 보고한다 (위원회 확인 대상, Go도 미구현).
 */
class EvaluationEngine(private val plan: AdmissionPlan) {

    private val rounding = plan.grading.rounding

    fun evaluate(roundCode: String, applicants: List<RoundApplicant>): RoundResult {
        val round = wrapInputError { plan.round(roundCode) }
        validateApplicants(round, applicants)

        // 1) 차수 점수 계산 — 수동 점수 미제출(미응시)자는 전형 대상에서 제외
        val scores = applicants.associate { it.id to roundScore(round, it) }
        val absentees = applicants.filter { scores.getValue(it.id) == null }
        val active = applicants.filter { scores.getValue(it.id) != null }

        // 2) 편입 관계 위상 순서대로 전형별 선발
        val comparator = Comparator<RoundApplicant> { a, b ->
            scores.getValue(b.id)!!.compareTo(scores.getValue(a.id)!!)
        }.thenComparing(Tiebreakers.comparatorFor(round))

        val current = active.associateTo(HashMap()) { it.id to it.screening }
        val paths = active.associateTo(HashMap()) { it.id to mutableListOf(it.screening) }
        val passedIn = HashMap<String, String>()
        val carryIn = HashMap<String, Int>()

        for (screening in processOrder()) {
            val pool = active.filter { current[it.id] == screening.code && it.id !in passedIn }
            val limit = selectionLimit(round, screening) + (carryIn[screening.code] ?: 0)
            val sorted = pool.sortedWith(comparator)

            checkBoundaryTie(round, screening, sorted, limit, comparator)

            sorted.take(limit).forEach { passedIn[it.id] = screening.code }
            val fallback = screening.rejectedFallsTo ?: screening.overflowFallsTo
            sorted.drop(limit).forEach { loser ->
                if (fallback != null) {
                    current[loser.id] = fallback
                    paths.getValue(loser.id) += fallback
                }
            }

            if (round.selection is SelectionRule.Capacity && screening.unfilledGoesTo != null && pool.size < limit) {
                carryIn.merge(screening.unfilledGoesTo!!, limit - pool.size, Int::plus)
            }
        }

        // 3) 정원 외 "1차 합격자 최저점 이내" 확인 플래그
        val cutline = firstRoundCutline(round, passedIn, scores)

        val entries = applicants.map { applicant ->
            val score = scores.getValue(applicant.id)
            when {
                applicant in absentees -> RoundEntry(
                    applicantId = applicant.id,
                    outcome = RoundOutcome.ABSENT,
                    appliedScreening = null,
                    roundScore = null,
                    screeningPath = listOf(applicant.screening),
                )

                applicant.id in passedIn -> {
                    val screening = plan.screening(passedIn.getValue(applicant.id))
                    RoundEntry(
                        applicantId = applicant.id,
                        outcome = RoundOutcome.PASSED,
                        appliedScreening = screening.code,
                        roundScore = score,
                        screeningPath = paths.getValue(applicant.id),
                        belowFirstRoundCutline = cutline != null &&
                            screening.admitOnlyWithinFirstRoundCutline &&
                            score!! < cutline,
                    )
                }

                else -> RoundEntry(
                    applicantId = applicant.id,
                    outcome = RoundOutcome.REJECTED,
                    appliedScreening = current.getValue(applicant.id),
                    roundScore = score,
                    screeningPath = paths.getValue(applicant.id),
                )
            }
        }
        return RoundResult(roundCode = round.code, entries = entries)
    }

    /**
     * 추가모집(정원 미달 시) 선발.
     *
     * 요강 8-바: 지원자 수가 모집정원에 미달한 경우 **일반전형으로만** 추가 모집하고,
     * **1차 전형 환산점수**에 의한 성적 상위자로 [openSeats]명을 선발한다.
     * 선발 기준 차수와 대상 전형은 plan의 [kr.hellogsm.entrance.plan.AdditionalRecruitmentPolicy]를 따른다.
     *
     * ⚠️ 기존 go-hellogsm에는 대응 배치가 없어(원서 상태 `RE_EVALUATE`만 존재) parity 검증 대상이
     * 아니다 — 요강만을 근거로 구현했다.
     *
     * @param openSeats 채워야 할 빈자리 수
     *   ([kr.hellogsm.entrance.engine.assignment.Vacancy.totalWithinCapacity])
     */
    fun evaluateAdditionalRecruitment(applicants: List<RoundApplicant>, openSeats: Int): RoundResult {
        val policy = plan.additionalRecruitment
            ?: throw EvaluationException("이 plan에는 추가모집 규칙이 선언되지 않음")
        if (openSeats < 0) throw EvaluationException("추가모집 선발 인원은 음수일 수 없음: $openSeats")

        val round = wrapInputError { plan.round(policy.basedOnRound) }
        validateApplicants(round, applicants)
        applicants.filterNot { it.screening in policy.screenings }.forEach {
            throw EvaluationException(
                "추가모집 대상 전형(${policy.screenings})이 아닌 지원자: '${it.id}'(${it.screening})",
            )
        }

        val scores = applicants.associate { it.id to roundScore(round, it) }
        val active = applicants.filter { scores.getValue(it.id) != null }
        val comparator = Comparator<RoundApplicant> { a, b ->
            scores.getValue(b.id)!!.compareTo(scores.getValue(a.id)!!)
        }.thenComparing(Tiebreakers.comparatorFor(round))

        val sorted = active.sortedWith(comparator)
        val screening = plan.screening(policy.screenings.first())
        checkBoundaryTie(round, screening, sorted, openSeats, comparator)
        val selected = sorted.take(openSeats).map(RoundApplicant::id).toSet()

        val entries = applicants.map { applicant ->
            val score = scores.getValue(applicant.id)
            RoundEntry(
                applicantId = applicant.id,
                outcome = when {
                    score == null -> RoundOutcome.ABSENT
                    applicant.id in selected -> RoundOutcome.PASSED
                    else -> RoundOutcome.REJECTED
                },
                appliedScreening = applicant.screening.takeIf { score != null },
                roundScore = score,
                screeningPath = listOf(applicant.screening),
            )
        }
        return RoundResult(roundCode = round.code, entries = entries)
    }

    // ── 차수 점수 계산 ────────────────────────────────────────────────

    /** 차수 점수. 필요한 수동 점수가 없으면(미응시) null */
    private fun roundScore(round: Round, applicant: RoundApplicant): BigDecimal? =
        when (val composition = round.score) {
            is ScoreComposition.Sum -> {
                val values = composition.components.map { componentValue(it, applicant) ?: return null }
                rounding.result(values.fold(BigDecimal.ZERO, BigDecimal::add))
            }

            is ScoreComposition.Weighted -> {
                val parts = composition.parts.map { part ->
                    val raw = componentValue(part.component, applicant) ?: return null
                    val normalized = part.normalizeTo?.let { normalizeTo ->
                        raw.multiply(normalizeTo)
                            .divide(componentMaxScore(part.component), rounding.intermediateScale, rounding.intermediateMode)
                    } ?: raw
                    normalized.multiply(part.weightPercent).divide(BigDecimal(100))
                }
                rounding.result(parts.fold(BigDecimal.ZERO, BigDecimal::add))
            }
        }

    private fun componentValue(component: ScoreComponent, applicant: RoundApplicant): BigDecimal? =
        when (component) {
            ScoreComponent.SubjectScore -> applicant.breakdown.subjectsScore
            ScoreComponent.AttendanceScore -> applicant.breakdown.attendanceScore
            ScoreComponent.VolunteerScore -> applicant.breakdown.volunteerScore

            is ScoreComponent.RoundScore ->
                applicant.previousRoundScores[component.roundCode]
                    ?: throw EvaluationException("지원자 '${applicant.id}'의 이전 차수 '${component.roundCode}' 점수가 없음")

            // 수동 점수 미제출 = 미응시
            is ScoreComponent.ManualScore -> applicant.manualScores[component.code]
        }

    /** 정규화(normalizeTo)의 분모가 되는 구성요소 만점 */
    private fun componentMaxScore(component: ScoreComponent): BigDecimal = when (component) {
        is ScoreComponent.RoundScore -> roundMaxScore(plan.round(component.roundCode))
        is ScoreComponent.ManualScore -> component.maxScore
        else -> throw EvaluationException("정규화를 지원하지 않는 구성요소: $component")
    }

    /**
     * 차수 만점. Sum 구성은 성적 총점 전체(교과+출석+봉사 = [kr.hellogsm.entrance.plan.Grading.totalMaxScore])를
     * 구성한다고 전제한다 (2026 기준 300점).
     */
    private fun roundMaxScore(round: Round): BigDecimal = when (val composition = round.score) {
        is ScoreComposition.Sum -> plan.grading.totalMaxScore
        is ScoreComposition.Weighted -> composition.maxScore
    }

    // ── 선발 규칙 ────────────────────────────────────────────────────

    /** 전형 처리 순서: 편입 출발 전형이 도착 전형보다 먼저. 순서가 같으면 plan 선언 순서 */
    private fun processOrder(): List<Screening> {
        val remaining = plan.screenings.toMutableList()
        val order = mutableListOf<Screening>()
        while (remaining.isNotEmpty()) {
            // 아직 처리되지 않은 전형이 편입해 들어오는 전형은 그 출발 전형이 처리될 때까지 미룬다
            val next = remaining.firstOrNull { candidate ->
                remaining.none { other ->
                    other != candidate && candidate.code in
                        listOfNotNull(other.rejectedFallsTo, other.overflowFallsTo, other.unfilledGoesTo)
                }
            } ?: throw EvaluationException(
                "전형 편입 관계에 순환이 있어 처리 순서를 정할 수 없음: ${remaining.map(Screening::code)}",
            )
            order += next
            remaining -= next
        }
        return order
    }

    /** 전형별 이 차수의 선발 인원 */
    private fun selectionLimit(round: Round, screening: Screening): Int {
        // 정원 외 전형은 배수 없이 항상 모집범위(정원)만큼
        if (!screening.withinCapacity) {
            return (screening.quota as Quota.Fixed).count
        }
        return when (val selection = round.selection) {
            is SelectionRule.Multiplier ->
                plan.resolvedQuota(screening.code).toBigDecimal()
                    .multiply(selection.value)
                    .setScale(0, RoundingMode.CEILING)
                    .toInt()

            SelectionRule.Capacity -> plan.resolvedQuota(screening.code)
        }
    }

    /** 선발 경계에 완전 동점이 걸리면 위원회 결정이 필요하므로 실패시킨다 */
    private fun checkBoundaryTie(
        round: Round,
        screening: Screening,
        sorted: List<RoundApplicant>,
        limit: Int,
        comparator: Comparator<RoundApplicant>,
    ) {
        if (limit <= 0 || sorted.size <= limit) return
        val lastPasser = sorted[limit - 1]
        val firstLoser = sorted[limit]
        if (comparator.compare(lastPasser, firstLoser) == 0) {
            val tied = sorted.filter { comparator.compare(it, lastPasser) == 0 }.map(RoundApplicant::id)
            throw UnresolvedTieException(round.code, screening.code, tied)
        }
    }

    /** 1차 차수일 때 정원 내 전형 합격자의 최저 점수 (정원 외 확인 플래그용) */
    private fun firstRoundCutline(
        round: Round,
        passedIn: Map<String, String>,
        scores: Map<String, BigDecimal?>,
    ): BigDecimal? {
        if (round.code != plan.rounds.first().code) return null
        if (plan.screenings.none(Screening::admitOnlyWithinFirstRoundCutline)) return null
        return passedIn
            .filterValues { plan.screening(it).withinCapacity }
            .keys
            .mapNotNull { scores[it] }
            .minOrNull()
    }

    // ── 입력 검증 ────────────────────────────────────────────────────

    private fun validateApplicants(round: Round, applicants: List<RoundApplicant>) {
        applicants.groupBy(RoundApplicant::id).filterValues { it.size > 1 }.keys.forEach {
            throw EvaluationException("지원자 식별자 중복: $it")
        }
        val manualMaxByCode = round.manualScores.associate { it.code to it.maxScore }
        for (applicant in applicants) {
            wrapInputError { plan.screening(applicant.screening) }
            applicant.manualScores.forEach { (code, value) ->
                val max = manualMaxByCode[code] ?: return@forEach
                if (value < BigDecimal.ZERO || value > max) {
                    throw EvaluationException(
                        "지원자 '${applicant.id}'의 수동 점수 '$code'($value)가 0~$max 범위를 벗어남",
                    )
                }
            }
        }
    }

    private fun <T> wrapInputError(block: () -> T): T =
        try {
            block()
        } catch (e: IllegalStateException) {
            throw EvaluationException(e.message ?: "잘못된 입력")
        }
}

private fun RoundingPolicy.result(value: BigDecimal): BigDecimal = value.setScale(resultScale, resultMode)
