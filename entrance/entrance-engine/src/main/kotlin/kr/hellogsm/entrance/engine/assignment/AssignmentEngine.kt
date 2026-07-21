package kr.hellogsm.entrance.engine.assignment

import kr.hellogsm.entrance.engine.evaluation.EvaluationException
import kr.hellogsm.entrance.engine.evaluation.RoundApplicant
import kr.hellogsm.entrance.engine.evaluation.Tiebreakers
import kr.hellogsm.entrance.engine.evaluation.UnresolvedTieException
import kr.hellogsm.entrance.plan.AdmissionPlan
import kr.hellogsm.entrance.plan.Screening
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 최종 합격자의 학과 배정과 예비 합격자 선정을 수행하는 엔진.
 *
 * 배정 규칙 (요강 8-라, go-hellogsm majorAssignmentJob과 동일):
 * - 정원 내(일반·특별) 합격자를 통합해 최종 성적 상위순으로 1지망 → 2지망 → 3지망 배정.
 * - 정원 외 합격자는 학과 정원과 별도 풀에서 배정하되 학과당 최대
 *   [kr.hellogsm.entrance.plan.MajorAssignmentPolicy.extraScreeningCapPerMajor]명.
 * - 배정 순서에 완전 동점(최종 점수와 모든 동점자 기준이 같음)이 있으면 배정 결과가
 *   순서에 따라 달라질 수 있으므로 [UnresolvedTieException]으로 실패시킨다.
 */
class AssignmentEngine(private val plan: AdmissionPlan) {

    /** 최종 학과 배정. [candidates]는 최종 차수 합격자 전원이어야 한다 */
    fun assign(candidates: List<FinalCandidate>): AssignmentResult {
        validate(candidates)
        val sorted = sortByFinalRank(candidates)

        val normalRemaining = plan.majors.associateTo(HashMap()) { it.code to it.capacity }
        val extraCap = plan.majorAssignment.extraScreeningCapPerMajor ?: Int.MAX_VALUE
        val extraRemaining = plan.majors.associateTo(HashMap()) { it.code to extraCap }

        val assignments = sorted.map { candidate ->
            val pool = if (screeningOf(candidate).withinCapacity) normalRemaining else extraRemaining
            val choiceIndex = candidate.majorChoices.indexOfFirst { (pool[it] ?: 0) > 0 }
            if (choiceIndex < 0) {
                throw EvaluationException(
                    "지원자 '${candidate.applicant.id}'를 지망 학과(${candidate.majorChoices})에 배정할 수 없음 — " +
                        "모든 지망 학과의 배정 가능 인원이 소진됨",
                )
            }
            val major = candidate.majorChoices[choiceIndex]
            pool[major] = pool.getValue(major) - 1
            MajorAssignment(
                applicantId = candidate.applicant.id,
                screening = candidate.applicant.screening,
                majorCode = major,
                choiceRank = choiceIndex + 1,
            )
        }
        return AssignmentResult(assignments)
    }

    /**
     * 예비 합격자 선정: 대상 전형([kr.hellogsm.entrance.plan.WaitlistPolicy.fromScreening])의
     * 최종 불합격자 중 고득점순으로 모집정원의 n% 범위(내림) 이내.
     */
    fun waitlist(rejectedCandidates: List<FinalCandidate>): List<String> {
        val policy = plan.waitlist ?: return emptyList()
        val count = plan.totalCapacity.toBigDecimal()
            .multiply(policy.percentOfTotalCapacity)
            .divide(BigDecimal(100))
            .setScale(0, RoundingMode.DOWN)
            .toInt()

        val eligible = rejectedCandidates.filter { it.applicant.screening == policy.fromScreening }
        return sortByFinalRank(eligible).take(count).map { it.applicant.id }
    }

    /**
     * 합격자 미등록·합격 취소(중도포기)로 생긴 빈자리에 예비 합격자를 성적순으로 추가 배정한다.
     *
     * 이미 배정된 합격자의 학과는 바꾸지 않고 비워진 자리만 채운다
     * (go-hellogsm `majorAssignmentJob`의 `ADDITIONAL_ASSIGNED` 모드와 같은 방식).
     * 빈자리는 포기자가 차지하고 있던 학과에 생기며, 정원 내·정원 외 자리는 서로 넘나들지 않는다.
     *
     * ⚠️ go-hellogsm과의 의도된 차이: 기존 구현은 정원 내 포기자가 생겨도 같은 학과의 정원 외
     * 자리가 함께 열리도록 계산한다(`major_assignment_job.go`의 `ExtraMajor-extra-normal`).
     * 정원 외 상한(학과당 2명)은 정원 내와 독립이므로 이는 상한을 넘길 수 있는 버그로 보고,
     * 엔진은 각 풀의 빈자리만 채운다 — PLAN.md 7절 2항(요강이 정답).
     */
    fun reassign(
        previous: AssignmentResult,
        withdrawnApplicantIds: Collection<String>,
        promotionCandidates: List<FinalCandidate>,
    ): ReassignmentResult {
        val assignedById = previous.assignments.associateBy(MajorAssignment::applicantId)
        val withdrawn = withdrawnApplicantIds.toSet()

        withdrawn.filterNot(assignedById::containsKey).forEach {
            throw EvaluationException("중도포기자 '$it'가 기존 배정 결과에 없음")
        }
        promotionCandidates.filter { assignedById.containsKey(it.applicant.id) }.forEach {
            throw EvaluationException("추가 배정 대상 '${it.applicant.id}'가 이미 학과에 배정되어 있음")
        }

        // 포기자가 비운 자리를 정원 내·정원 외 풀별로 집계
        val openWithinCapacity = HashMap<String, Int>()
        val openExtra = HashMap<String, Int>()
        withdrawn.map(assignedById::getValue).forEach { vacated ->
            val pool = if (plan.screening(vacated.screening).withinCapacity) openWithinCapacity else openExtra
            pool.merge(vacated.majorCode, 1, Int::plus)
        }

        val promoted = sortByFinalRank(promotionCandidates).mapNotNull { candidate ->
            val pool = if (screeningOf(candidate).withinCapacity) openWithinCapacity else openExtra
            val choiceIndex = candidate.majorChoices.indexOfFirst { (pool[it] ?: 0) > 0 }
            // 지망 학과에 빈자리가 없으면 승격하지 않고 예비 합격 상태로 남는다
            if (choiceIndex < 0) return@mapNotNull null

            val major = candidate.majorChoices[choiceIndex]
            pool[major] = pool.getValue(major) - 1
            MajorAssignment(
                applicantId = candidate.applicant.id,
                screening = candidate.applicant.screening,
                majorCode = major,
                choiceRank = choiceIndex + 1,
            )
        }

        return ReassignmentResult(
            assignments = previous.assignments.filterNot { it.applicantId in withdrawn } + promoted,
            promoted = promoted,
            withdrawn = withdrawn.toList(),
            unfilled = Vacancy(
                withinCapacity = openWithinCapacity.filterValues { it > 0 },
                extra = openExtra.filterValues { it > 0 },
            ),
        )
    }

    /** 최종 성적 상위순 + 최종 차수 동점자 기준. 완전 동점이 남으면 실패 */
    private fun sortByFinalRank(candidates: List<FinalCandidate>): List<FinalCandidate> {
        val finalRound = plan.rounds.last()
        val comparator = Comparator<FinalCandidate> { a, b -> b.finalScore.compareTo(a.finalScore) }
            .thenComparing({ it.applicant }, Tiebreakers.comparatorFor(finalRound))

        val sorted = candidates.sortedWith(comparator)
        sorted.zipWithNext().forEach { (a, b) ->
            if (comparator.compare(a, b) == 0) {
                val tied = sorted.filter { comparator.compare(it, a) == 0 }.map { it.applicant.id }
                throw UnresolvedTieException(finalRound.code, null, tied)
            }
        }
        return sorted
    }

    private fun screeningOf(candidate: FinalCandidate): Screening =
        try {
            plan.screening(candidate.applicant.screening)
        } catch (e: IllegalStateException) {
            throw EvaluationException(e.message ?: "잘못된 전형 코드")
        }

    private fun validate(candidates: List<FinalCandidate>) {
        candidates.groupBy { it.applicant.id }.filterValues { it.size > 1 }.keys.forEach {
            throw EvaluationException("지원자 식별자 중복: $it")
        }

        val majorCodes = plan.majors.map { it.code }.toSet()
        val choiceCount = plan.majorAssignment.choiceCount
        for (candidate in candidates) {
            val choices = candidate.majorChoices
            if (choices.size != choiceCount) {
                throw EvaluationException(
                    "지원자 '${candidate.applicant.id}'의 지망 학과 수(${choices.size})가 $choiceCount 개가 아님 — 전부 기재 필수",
                )
            }
            if (choices.distinct().size != choices.size) {
                throw EvaluationException("지원자 '${candidate.applicant.id}'의 지망 학과에 중복이 있음: $choices")
            }
            choices.filterNot(majorCodes::contains).forEach {
                throw EvaluationException("지원자 '${candidate.applicant.id}'의 지망 학과가 존재하지 않음: $it")
            }
        }

        val normalCount = candidates.count { screeningOf(it).withinCapacity }
        if (normalCount > plan.totalCapacity) {
            throw EvaluationException(
                "정원 내 최종 합격자 수($normalCount)가 총정원(${plan.totalCapacity})을 초과함",
            )
        }
    }
}

/** 학과 배정 대상자 (최종 차수 합격자) */
data class FinalCandidate(
    /** 최종 적용 전형이 반영된 지원자 ([RoundApplicant.screening] = 최종 차수 결과의 적용 전형) */
    val applicant: RoundApplicant,
    /** 최종 차수 점수 ([kr.hellogsm.entrance.engine.evaluation.RoundEntry.roundScore]) */
    val finalScore: BigDecimal,
    /** 지망 학과 코드, 지망 순서대로 */
    val majorChoices: List<String>,
)

data class AssignmentResult(
    /** 성적 상위순 배정 결과 */
    val assignments: List<MajorAssignment>,
) {
    fun byMajor(majorCode: String): List<MajorAssignment> = assignments.filter { it.majorCode == majorCode }
}

data class MajorAssignment(
    val applicantId: String,
    val screening: String,
    val majorCode: String,
    /** 몇 지망으로 배정됐는지 (1지망 = 1) */
    val choiceRank: Int,
)

/** 중도포기 발생 후 재배정 결과 */
data class ReassignmentResult(
    /** 재배정 후 전체 배정 현황 (유지된 합격자 + 추가 배정자) */
    val assignments: List<MajorAssignment>,
    /** 이번에 추가로 배정된 예비 합격자 */
    val promoted: List<MajorAssignment>,
    val withdrawn: List<String>,
    /** 예비 합격자로도 채우지 못한 빈자리 — 정원 미달이면 추가모집 대상이 된다 */
    val unfilled: Vacancy,
)

/** 학과별 빈자리 (풀별로 분리) */
data class Vacancy(
    val withinCapacity: Map<String, Int>,
    val extra: Map<String, Int>,
) {
    /** 정원 내 빈자리 총합 — 추가모집 선발 인원의 기준 */
    val totalWithinCapacity: Int = withinCapacity.values.sum()
}
