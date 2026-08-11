package kr.hellogsm.entrance.plan

import java.math.BigDecimal
import java.math.RoundingMode

class PlanValidationException(val errors: List<String>) : IllegalStateException(
    "입학전형 plan 검증 실패 (${errors.size}건):\n" + errors.joinToString("\n") { " - $it" },
)

/**
 * [AdmissionPlan] 생성 시점의 정합성 검증. 모든 오류를 모아 한 번에 보고한다.
 */
internal object PlanValidator {
    fun validate(plan: AdmissionPlan) {
        val errors = mutableListOf<String>()
        validateMajors(plan, errors)
        validateScreenings(plan, errors)
        validateGrading(plan, errors)
        validateRounds(plan, errors)
        validatePolicies(plan, errors)
        validateSchedule(plan, errors)
        if (errors.isNotEmpty()) throw PlanValidationException(errors)
    }

    private fun validateMajors(plan: AdmissionPlan, errors: MutableList<String>) {
        if (plan.majors.isEmpty()) errors += "학과가 하나 이상 필요함"
        plan.majors.groupBy(Major::code).filterValues { it.size > 1 }.keys.forEach {
            errors += "학과 코드 중복: $it"
        }
        plan.majors.filter { it.capacity <= 0 }.forEach {
            errors += "학과 '${it.code}'의 정원은 양수여야 함: ${it.capacity}"
        }
    }

    private fun validateScreenings(plan: AdmissionPlan, errors: MutableList<String>) {
        if (plan.screenings.isEmpty()) errors += "전형이 하나 이상 필요함"
        plan.screenings.groupBy(Screening::code).filterValues { it.size > 1 }.keys.forEach {
            errors += "전형 코드 중복: $it"
        }

        val codes = plan.screenings.map(Screening::code).toSet()
        for (screening in plan.screenings) {
            listOf(
                "unfilledGoesTo" to screening.unfilledGoesTo,
                "rejectedFallsTo" to screening.rejectedFallsTo,
                "overflowFallsTo" to screening.overflowFallsTo,
            ).forEach { (field, target) ->
                if (target != null && target !in codes) {
                    errors += "전형 '${screening.code}'의 $field 대상이 존재하지 않음: $target"
                }
                if (target == screening.code) {
                    errors += "전형 '${screening.code}'의 $field 이 자기 자신을 가리킴"
                }
            }
            if (screening.admitOnlyWithinFirstRoundCutline && screening.withinCapacity) {
                errors += "전형 '${screening.code}': admitOnlyWithinFirstRoundCutline은 정원 외 전형에만 적용 가능"
            }

            when (val quota = screening.quota) {
                is Quota.Fixed -> {
                    if (quota.count < 0) errors += "전형 '${screening.code}'의 정원은 음수일 수 없음: ${quota.count}"
                    val cap = quota.capPercentOfTotal
                    if (cap != null) {
                        if (cap <= BigDecimal.ZERO || cap > BigDecimal(100)) {
                            errors += "전형 '${screening.code}'의 정원 상한 비율이 0~100 범위를 벗어남: $cap%"
                        } else {
                            // "전체 모집정원의 n% 이내" — 인원수이므로 내림
                            val maxCount = cap.multiply(plan.totalCapacity.toBigDecimal())
                                .divide(BigDecimal(100))
                                .setScale(0, RoundingMode.DOWN)
                                .toInt()
                            if (quota.count > maxCount) {
                                errors += "전형 '${screening.code}'의 정원 ${quota.count}명이 총정원의 $cap% 상한($maxCount 명)을 초과함"
                            }
                        }
                    }
                }

                Quota.Remainder -> if (!screening.withinCapacity) {
                    errors += "정원 외 전형 '${screening.code}'는 Remainder 정원을 가질 수 없음"
                }
            }
        }

        val regulars = plan.regularScreenings
        val remainders = regulars.count { it.quota is Quota.Remainder }
        if (remainders > 1) errors += "Remainder 정원을 갖는 정원 내 전형은 최대 1개여야 함 (현재 $remainders 개)"

        val fixedSum = plan.regularFixedQuota
        if (fixedSum > plan.totalCapacity) {
            errors += "정원 내 전형의 고정 정원 합($fixedSum)이 총정원(${plan.totalCapacity})을 초과함"
        }
        if (remainders == 0 && regulars.isNotEmpty() && fixedSum != plan.totalCapacity) {
            errors += "Remainder 전형이 없으면 정원 내 고정 정원 합($fixedSum)이 총정원(${plan.totalCapacity})과 같아야 함"
        }
    }

    private fun validateGrading(plan: AdmissionPlan, errors: MutableList<String>) {
        if (plan.grading.schemes.isEmpty()) errors += "성적 산출 스키마가 하나 이상 필요함"

        val maxScores = plan.grading.schemes.mapValues { it.value.maxScore }
        if (maxScores.values.map(BigDecimal::stripTrailingZeros).distinct().size > 1) {
            errors += "졸업 구분별 성적 총점 만점이 서로 다름: " +
                maxScores.entries.joinToString { "${it.key}=${it.value}" }
        }

        for ((type, scheme) in plan.grading.schemes) {
            when (scheme) {
                is TranscriptGrading -> validateTranscript(type, scheme, errors)
                is FormulaGrading -> validateFormula(type, scheme, errors)
            }
        }
    }

    private fun validateTranscript(type: GraduationType, scheme: TranscriptGrading, errors: MutableList<String>) {
        val general = scheme.generalSubjects
        if (general.semesterPoints.isEmpty()) {
            errors += "[$type] 일반교과 학기별 배점이 하나 이상 필요함"
        } else {
            val sum = general.semesterPoints.values.reduce(BigDecimal::add)
            if (sum.compareTo(general.maxScore) != 0) {
                errors += "[$type] 일반교과 학기별 배점 합($sum)이 만점(${general.maxScore})과 다름"
            }
        }
        if (general.achievementPoints.isEmpty()) errors += "[$type] 일반교과 성취도 환산표가 필요함"
        if (scheme.artsSubjects.achievementPoints.isEmpty()) errors += "[$type] 예체능 성취도 환산표가 필요함"

        val attendance = scheme.attendance
        if (attendance.years.isEmpty()) errors += "[$type] 출석: 반영 학년이 하나 이상 필요함"
        if (attendance.years.size != attendance.years.distinct().size) errors += "[$type] 출석: 반영 학년 중복"
        if (attendance.latenessPerAbsenceDay <= 0) {
            errors += "[$type] 출석: 지각·조퇴·결과의 결석 환산 기준은 양수여야 함"
        }
        if (attendance.zeroFromAbsenceDays <= 0) {
            errors += "[$type] 출석: 0점 처리 기준 일수는 양수여야 함"
        }

        val volunteer = scheme.volunteer
        if (volunteer.years.isEmpty()) errors += "[$type] 봉사: 반영 학년이 하나 이상 필요함"
        if (volunteer.years.size != volunteer.years.distinct().size) errors += "[$type] 봉사: 반영 학년 중복"
        if (volunteer.steps.isEmpty()) {
            errors += "[$type] 봉사: 계단 기준이 하나 이상 필요함"
        } else {
            if (volunteer.steps != volunteer.steps.sortedByDescending(VolunteerStep::minHours)) {
                errors += "[$type] 봉사: 계단 기준은 minHours 내림차순이어야 함"
            }
            volunteer.steps.filter { it.points > volunteer.maxScorePerYear }.forEach {
                errors += "[$type] 봉사: 계단 점수(${it.points})가 연간 만점(${volunteer.maxScorePerYear})을 초과함"
            }
        }
    }

    private fun validateFormula(type: GraduationType, scheme: FormulaGrading, errors: MutableList<String>) {
        listOf("교과" to scheme.subjectFormula, "봉사" to scheme.volunteerFormula).forEach { (label, formula) ->
            if (formula.maxInput <= formula.minInput) {
                errors += "[$type] $label 환산식: maxInput(${formula.maxInput})은 minInput(${formula.minInput})보다 커야 함"
            }
            if (formula.maxScore <= BigDecimal.ZERO) {
                errors += "[$type] $label 환산식: maxScore는 양수여야 함"
            }
        }
    }

    private fun validateRounds(plan: AdmissionPlan, errors: MutableList<String>) {
        if (plan.rounds.isEmpty()) errors += "전형 차수가 하나 이상 필요함"
        plan.rounds.groupBy(Round::code).filterValues { it.size > 1 }.keys.forEach {
            errors += "전형 차수 코드 중복: $it"
        }

        val manualCodes = plan.rounds.flatMap(Round::manualScores).map(ScoreComponent.ManualScore::code)
        manualCodes.groupBy { it }.filterValues { it.size > 1 }.keys.forEach {
            errors += "수동 입력 점수 코드 중복: $it"
        }

        plan.rounds.forEachIndexed { index, round ->
            val selection = round.selection
            if (selection is SelectionRule.Multiplier && selection.value <= BigDecimal.ZERO) {
                errors += "차수 '${round.code}': 선발 배수는 양수여야 함: ${selection.value}"
            }

            when (val score = round.score) {
                is ScoreComposition.Sum -> if (score.components.isEmpty()) {
                    errors += "차수 '${round.code}': 점수 구성요소가 하나 이상 필요함"
                }

                is ScoreComposition.Weighted -> {
                    if (score.parts.isEmpty()) {
                        errors += "차수 '${round.code}': 가중 점수 구성요소가 하나 이상 필요함"
                    } else {
                        val weightSum = score.parts.map { it.weightPercent }.reduce(BigDecimal::add)
                        if (weightSum.compareTo(BigDecimal(100)) != 0) {
                            errors += "차수 '${round.code}': 반영 비율 합($weightSum%)이 100%가 아님"
                        }
                    }
                    if (score.maxScore <= BigDecimal.ZERO) {
                        errors += "차수 '${round.code}': 만점은 양수여야 함"
                    }
                }
            }

            val earlierRounds = plan.rounds.take(index).map(Round::code).toSet()
            round.scoreComponents.filterIsInstance<ScoreComponent.RoundScore>().forEach {
                if (it.roundCode !in earlierRounds) {
                    errors += "차수 '${round.code}': 이전 차수가 아닌 '${it.roundCode}'의 점수를 참조함"
                }
            }
            round.manualScores.filter { it.maxScore <= BigDecimal.ZERO }.forEach {
                errors += "차수 '${round.code}': 수동 입력 점수 '${it.code}'의 만점은 양수여야 함"
            }

            val roundManualCodes = round.manualScores.map(ScoreComponent.ManualScore::code).toSet()
            round.tiebreakers.forEach { tiebreaker ->
                when (tiebreaker) {
                    is Tiebreaker.ManualScore -> if (tiebreaker.code !in roundManualCodes) {
                        errors += "차수 '${round.code}': 동점자 기준이 이 차수에 선언되지 않은 수동 점수 '${tiebreaker.code}'를 참조함"
                    }

                    is Tiebreaker.SemesterScores -> if (tiebreaker.order.isEmpty()) {
                        errors += "차수 '${round.code}': 학기 동점자 기준에 학기가 하나 이상 필요함"
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun validatePolicies(plan: AdmissionPlan, errors: MutableList<String>) {
        val assignment = plan.majorAssignment
        if (assignment.choiceCount !in 1..plan.majors.size.coerceAtLeast(1)) {
            errors += "지망 학과 수(${assignment.choiceCount})는 1~학과 수(${plan.majors.size}) 범위여야 함"
        }
        if (assignment.extraScreeningCapPerMajor != null && assignment.extraScreeningCapPerMajor <= 0) {
            errors += "정원 외 학과당 배정 상한은 양수여야 함: ${assignment.extraScreeningCapPerMajor}"
        }

        val screeningCodes = plan.screenings.map(Screening::code).toSet()
        val roundCodes = plan.rounds.map(Round::code).toSet()

        plan.waitlist?.let { waitlist ->
            if (waitlist.percentOfTotalCapacity <= BigDecimal.ZERO || waitlist.percentOfTotalCapacity > BigDecimal(100)) {
                errors += "예비합격 비율이 0~100 범위를 벗어남: ${waitlist.percentOfTotalCapacity}%"
            }
            if (waitlist.fromScreening !in screeningCodes) {
                errors += "예비합격 대상 전형이 존재하지 않음: ${waitlist.fromScreening}"
            }
        }

        plan.additionalRecruitment?.let { additional ->
            if (additional.screenings.isEmpty()) errors += "추가모집 전형이 하나 이상 필요함"
            additional.screenings.filter { it !in screeningCodes }.forEach {
                errors += "추가모집 전형이 존재하지 않음: $it"
            }
            if (additional.basedOnRound !in roundCodes) {
                errors += "추가모집 기준 차수가 존재하지 않음: ${additional.basedOnRound}"
            }
        }
    }

    private fun validateSchedule(plan: AdmissionPlan, errors: MutableList<String>) {
        plan.schedule.filter { it.end < it.start }.forEach {
            errors += "일정 '${it.code}': 종료일(${it.end})이 시작일(${it.start})보다 빠름"
        }
        plan.schedule.groupBy(ScheduleEvent::code).filterValues { it.size > 1 }.keys.forEach {
            errors += "일정 코드 중복: $it"
        }
    }
}
