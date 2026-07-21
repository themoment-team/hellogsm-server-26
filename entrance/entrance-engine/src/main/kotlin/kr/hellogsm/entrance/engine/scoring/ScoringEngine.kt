package kr.hellogsm.entrance.engine.scoring

import kr.hellogsm.entrance.plan.Achievement
import kr.hellogsm.entrance.plan.AdmissionPlan
import kr.hellogsm.entrance.plan.ArtsSubjectRule
import kr.hellogsm.entrance.plan.AttendanceRule
import kr.hellogsm.entrance.plan.FormulaGrading
import kr.hellogsm.entrance.plan.GeneralSubjectRule
import kr.hellogsm.entrance.plan.MissingSemesterStrategy
import kr.hellogsm.entrance.plan.RangeScaleFormula
import kr.hellogsm.entrance.plan.RoundingPolicy
import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plan.TranscriptGrading
import kr.hellogsm.entrance.plan.VolunteerRule
import java.math.BigDecimal

/**
 * plan의 성적 산출 규칙([kr.hellogsm.entrance.plan.Grading])을 해석해
 * 지원자 성적 원본([StudentRecord])을 환산점수([ScoreBreakdown])로 변환하는 순수 함수 엔진.
 *
 * 같은 plan + 같은 입력이면 항상 같은 출력을 낸다. 모든 연산은 BigDecimal이며
 * 반올림은 plan의 [RoundingPolicy]를 따른다.
 */
class ScoringEngine(private val plan: AdmissionPlan) {

    private val rounding = plan.grading.rounding

    fun score(record: StudentRecord): ScoreBreakdown = when (record) {
        is StudentRecord.Transcript -> scoreTranscript(record)
        is StudentRecord.Ged -> scoreGed(record)
    }

    // ── 내신 기반 산출 (졸업예정자/졸업자) ──────────────────────────────

    private fun scoreTranscript(record: StudentRecord.Transcript): ScoreBreakdown {
        val scheme = plan.grading.scheme(record.graduationType) as? TranscriptGrading
            ?: throw ScoringException("${record.graduationType}의 산출 스키마가 내신 기반이 아님")

        val resolved = resolveSemesters(scheme.generalSubjects, record.generalAchievements)
        val semesterScores = resolved.mapValues { (semester, data) ->
            semesterScore(scheme.generalSubjects, data.achievements, scheme.generalSubjects.semesterPoints.getValue(semester))
        }
        val generalScore = rounding.result(semesterScores.values.fold(BigDecimal.ZERO, BigDecimal::add))
        val artsScore = artsScore(scheme.artsSubjects, record.artsAchievements)

        val attendanceMissingYears = scheme.attendance.years.filterNot(record.attendanceByYear::containsKey)
        val attendanceScore = attendanceScore(scheme.attendance, record.attendanceByYear, attendanceMissingYears)

        val volunteerMissingYears = scheme.volunteer.years.filterNot(record.volunteerHoursByYear::containsKey)
        val volunteerScore = volunteerScore(scheme.volunteer, record.volunteerHoursByYear, volunteerMissingYears)

        val subjectsScore = rounding.result(generalScore + artsScore)
        val nonSubjectsScore = rounding.result(attendanceScore + volunteerScore)

        return ScoreBreakdown(
            graduationType = record.graduationType,
            subjectsScore = subjectsScore,
            attendanceScore = attendanceScore,
            volunteerScore = volunteerScore,
            nonSubjectsScore = nonSubjectsScore,
            totalScore = rounding.result(subjectsScore + nonSubjectsScore),
            transcriptDetail = TranscriptDetail(
                generalSubjectsScore = generalScore,
                artsSubjectsScore = artsScore,
                semesterScores = semesterScores,
                semesterSources = resolved.mapValues { it.value.source },
                attendanceDefaultedYears = attendanceMissingYears,
                volunteerDefaultedYears = volunteerMissingYears,
            ),
        )
    }

    /**
     * 결측 학기 대체. 요강의 우선순위를 [GeneralSubjectRule.missingSemesterFallback] 선언 순서로 적용한다.
     *
     * 상위 학년부터 해석하므로, 하위 학년이 차상위 학년을 참조할 때는 이미 대체가 끝난 성적을
     * 이어받는다(예: 2-2가 2-1로 대체된 상태에서 1-2가 차상위 학년을 참조하면 그 값을 사용).
     * 반면 '같은 학년 다른 학기'는 요강상 두 학기가 모두 없으면 성립하지 않는 규칙이므로
     * 제출된 원본 성적만 참조한다.
     */
    private fun resolveSemesters(
        rule: GeneralSubjectRule,
        submitted: Map<SemesterRef, List<Achievement>>,
    ): Map<SemesterRef, ResolvedSemester> {
        val cleaned = submitted.filterValues { it.isNotEmpty() }
        val resolved = linkedMapOf<SemesterRef, ResolvedSemester>()

        val slots = rule.semesterPoints.keys
        val descending = slots.sortedWith(compareByDescending<SemesterRef> { it.year }.thenByDescending { it.semester })

        for (slot in descending) {
            val own = cleaned[slot]
            if (own != null) {
                resolved[slot] = ResolvedSemester(own, SemesterSource.Submitted)
                continue
            }

            val substitute = rule.missingSemesterFallback.firstNotNullOfOrNull { strategy ->
                findSubstitute(slot, strategy, cleaned, resolved)?.let { (from, achievements) ->
                    ResolvedSemester(achievements, SemesterSource.Substituted(from, strategy))
                }
            } ?: throw ScoringException("결측 학기 $slot 을 대체할 성적이 없음 — 입학전형위원회 산출 대상(수동 오버라이드 필요)")
            resolved[slot] = substitute
        }

        // 반환은 plan 선언 순서
        return slots.associateWith(resolved::getValue)
    }

    private fun findSubstitute(
        slot: SemesterRef,
        strategy: MissingSemesterStrategy,
        submitted: Map<SemesterRef, List<Achievement>>,
        resolved: Map<SemesterRef, ResolvedSemester>,
    ): Pair<SemesterRef, List<Achievement>>? = when (strategy) {
        MissingSemesterStrategy.SAME_YEAR_OTHER_SEMESTER -> {
            val other = SemesterRef(slot.year, if (slot.semester == 1) 2 else 1)
            submitted[other]?.let { other to it }
        }

        MissingSemesterStrategy.UPPER_YEAR ->
            if (slot.year >= 3) null
            else lookup(SemesterRef(slot.year + 1, slot.semester), submitted, resolved)

        MissingSemesterStrategy.LOWER_YEAR ->
            if (slot.year <= 1) null
            else lookup(SemesterRef(slot.year - 1, slot.semester), submitted, resolved)
    }

    private fun lookup(
        source: SemesterRef,
        submitted: Map<SemesterRef, List<Achievement>>,
        resolved: Map<SemesterRef, ResolvedSemester>,
    ): Pair<SemesterRef, List<Achievement>>? =
        (resolved[source]?.achievements ?: submitted[source])?.let { source to it }

    /**
     * 학기별 일반교과 성적: 성취도 환산점수 합 ÷ (과목수 × 최고 환산점수)를
     * 중간값 반올림(scale 5)한 뒤 학기 배점을 곱하고 결과값 반올림(scale 3)한다.
     */
    private fun semesterScore(
        rule: GeneralSubjectRule,
        achievements: List<Achievement>,
        semesterPoints: BigDecimal,
    ): BigDecimal {
        val sum = achievements.sumOf { achievement ->
            rule.achievementPoints[achievement]
                ?: throw ScoringException("일반교과 성취도 $achievement 의 환산점수가 정의되지 않음")
        }
        val maxPoint = rule.achievementPoints.values.max()
        val ratio = sum.toBigDecimal()
            .divide((achievements.size * maxPoint).toBigDecimal(), rounding.intermediateScale, rounding.intermediateMode)
        return rounding.result(ratio * semesterPoints)
    }

    /**
     * 예체능 교과 성적: 3년간 성취도 환산점수의 평균점(합 ÷ (총개수 × 최고 환산점수))에
     * 만점을 곱한다. 요강에 따라 평균점과 결과값 모두 소수점 넷째 자리에서 반올림(scale 3)한다.
     */
    private fun artsScore(rule: ArtsSubjectRule, achievements: List<Achievement>): BigDecimal {
        if (achievements.isEmpty()) throw ScoringException("예체능 교과 성취도가 최소 1개 필요함")
        val sum = achievements.sumOf { achievement ->
            rule.achievementPoints[achievement]
                ?: throw ScoringException("예체능 성취도 $achievement 의 환산점수가 정의되지 않음 (유효: ${rule.achievementPoints.keys})")
        }
        val maxPoint = rule.achievementPoints.values.max()
        val average = sum.toBigDecimal()
            .divide((achievements.size * maxPoint).toBigDecimal(), rounding.resultScale, rounding.resultMode)
        return rounding.result(average * rule.maxScore)
    }

    /**
     * 출석 성적: 전 학년 미인정 출결을 합산해
     * 환산 결석일수 = 결석 + ⌊(지각+조퇴+결과) ÷ 기준횟수⌋ 를 구하고,
     * 만점에서 1일당 감점을 뺀다 (기준 일수 이상이면 0점, 음수도 0점).
     *
     * 결측 학년이 있으면 그 학년에는 기본점을 부여하고, 감점식은 자료가 있는 학년의
     * 만점 몫(만점 × 있는 학년 수 ÷ 전체 학년 수)에만 적용한다.
     * (요강은 결측 학년 기본점 5점만 명시하므로, 학년당 10점 몫이라는 해석을 따른다 —
     * 기존 Go 구현은 이 경우를 지원하지 않아 parity 대상이 아님)
     */
    private fun attendanceScore(
        rule: AttendanceRule,
        byYear: Map<Int, AttendanceRecord>,
        missingYears: List<Int>,
    ): BigDecimal {
        byYear.keys.filterNot(rule.years::contains).forEach {
            throw ScoringException("출결 반영 학년(${rule.years})이 아닌 학년의 자료: $it 학년")
        }
        val present = byYear.values
        val equivalentAbsenceDays =
            present.sumOf(AttendanceRecord::absenceDays) +
                present.sumOf(AttendanceRecord::latenessTotal) / rule.latenessPerAbsenceDay

        val presentMax = rule.maxScore
            .multiply(present.size.toBigDecimal())
            .divide(rule.years.size.toBigDecimal(), rounding.intermediateScale, rounding.intermediateMode)
        val deducted =
            if (equivalentAbsenceDays >= rule.zeroFromAbsenceDays) BigDecimal.ZERO
            else (presentMax - rule.deductionPerAbsenceDay * equivalentAbsenceDays.toBigDecimal())
                .coerceAtLeast(BigDecimal.ZERO)

        return rounding.result(deducted + rule.missingYearDefault * missingYears.size.toBigDecimal())
    }

    /** 봉사활동 성적: 학년별 연간 시간을 계단 기준에 매칭해 합산. 결측 학년은 기본점 부여 */
    private fun volunteerScore(
        rule: VolunteerRule,
        hoursByYear: Map<Int, Int>,
        missingYears: List<Int>,
    ): BigDecimal {
        hoursByYear.keys.filterNot(rule.years::contains).forEach {
            throw ScoringException("봉사 반영 학년(${rule.years})이 아닌 학년의 자료: $it 학년")
        }
        val total = rule.years.sumOf { year ->
            when (val hours = hoursByYear[year]) {
                null -> rule.missingYearDefault
                else -> rule.steps.firstOrNull { hours >= it.minHours }?.points ?: rule.floorScore
            }
        }
        return rounding.result(total)
    }

    // ── 수식 기반 산출 (검정고시) ──────────────────────────────────────

    private fun scoreGed(record: StudentRecord.Ged): ScoreBreakdown {
        val scheme = plan.grading.scheme(record.graduationType) as? FormulaGrading
            ?: throw ScoringException("${record.graduationType}의 산출 스키마가 수식 기반이 아님")

        val subjectsScore = scheme.subjectFormula.convert(record.averageScore)
        val attendanceScore = rounding.result(scheme.attendanceFixedScore)
        val volunteerScore = scheme.volunteerFormula.convert(record.averageScore)
        val nonSubjectsScore = rounding.result(attendanceScore + volunteerScore)

        return ScoreBreakdown(
            graduationType = record.graduationType,
            subjectsScore = subjectsScore,
            attendanceScore = attendanceScore,
            volunteerScore = volunteerScore,
            nonSubjectsScore = nonSubjectsScore,
            totalScore = rounding.result(subjectsScore + nonSubjectsScore),
            transcriptDetail = null,
        )
    }

    /**
     * (입력 − minInput) ÷ (maxInput − minInput) × maxScore.
     * 곱셈을 먼저 하고 마지막 나눗셈에서 결과값 자릿수로 반올림하므로,
     * 정확한 유리수 결과를 한 번만 반올림한 값과 같다 (기존 Go big.Rat 연산과 동일).
     * 입력이 minInput 미만이어서 음수가 되면 0점 처리한다.
     */
    private fun RangeScaleFormula.convert(input: BigDecimal): BigDecimal {
        if (input > maxInput) {
            throw ScoringException("환산식 입력($input)이 상한($maxInput)을 초과함")
        }
        return rounding.result(
            (input - minInput)
                .multiply(maxScore)
                .divide(maxInput - minInput, rounding.resultScale, rounding.resultMode)
                .coerceAtLeast(BigDecimal.ZERO),
        )
    }

    private data class ResolvedSemester(
        val achievements: List<Achievement>,
        val source: SemesterSource,
    )
}

private fun RoundingPolicy.result(value: BigDecimal): BigDecimal = value.setScale(resultScale, resultMode)
