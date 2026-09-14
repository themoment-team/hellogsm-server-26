package kr.hellogsm.entrance.dsl

import kr.hellogsm.entrance.plan.Achievement
import kr.hellogsm.entrance.plan.ArtsSubjectRule
import kr.hellogsm.entrance.plan.AttendanceRule
import kr.hellogsm.entrance.plan.FormulaGrading
import kr.hellogsm.entrance.plan.GeneralSubjectRule
import kr.hellogsm.entrance.plan.Grading
import kr.hellogsm.entrance.plan.GradingScheme
import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.MissingSemesterStrategy
import kr.hellogsm.entrance.plan.RangeScaleFormula
import kr.hellogsm.entrance.plan.RoundingPolicy
import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plan.TranscriptGrading
import kr.hellogsm.entrance.plan.VolunteerRule
import kr.hellogsm.entrance.plan.VolunteerStep
import java.math.BigDecimal
import java.math.RoundingMode

@AdmissionDsl
class GradingBuilder internal constructor() {
    private var rounding: RoundingPolicy? = null
    private val schemes = linkedMapOf<GraduationType, GradingScheme>()

    /** scale = 반올림 후 남기는 소수점 자릿수 */
    fun rounding(intermediateScale: Int, resultScale: Int, mode: RoundingMode = RoundingMode.HALF_UP) {
        rounding = RoundingPolicy(
            intermediateScale = intermediateScale,
            intermediateMode = mode,
            resultScale = resultScale,
            resultMode = mode,
        )
    }

    /** 내신 기반 산출 (졸업예정자/졸업자) */
    fun transcript(type: GraduationType, block: TranscriptGradingBuilder.() -> Unit) {
        schemes[type] = TranscriptGradingBuilder(type).apply(block).build()
    }

    /** 수식 기반 산출 (검정고시) */
    fun formula(type: GraduationType, block: FormulaGradingBuilder.() -> Unit) {
        schemes[type] = FormulaGradingBuilder(type).apply(block).build()
    }

    internal fun build(): Grading = Grading(
        rounding = requireNotNull(rounding) { "grading.rounding 선언은 필수" },
        schemes = schemes.toMap(),
    )
}

@AdmissionDsl
class TranscriptGradingBuilder internal constructor(private val type: GraduationType) {
    private var generalSubjects: GeneralSubjectRule? = null
    private var artsSubjects: ArtsSubjectRule? = null
    private var attendance: AttendanceRule? = null
    private var volunteer: VolunteerRule? = null

    fun generalSubjects(max: Int, block: GeneralSubjectsBuilder.() -> Unit) {
        generalSubjects = GeneralSubjectsBuilder(max).apply(block).build()
    }

    fun artsSubjects(max: Int, block: ArtsSubjectsBuilder.() -> Unit) {
        artsSubjects = ArtsSubjectsBuilder(max).apply(block).build()
    }

    fun attendance(max: Int, years: List<Int> = listOf(1, 2, 3), block: AttendanceBuilder.() -> Unit) {
        attendance = AttendanceBuilder(max, years).apply(block).build()
    }

    fun volunteer(maxPerYear: Int, years: List<Int> = listOf(1, 2, 3), block: VolunteerBuilder.() -> Unit) {
        volunteer = VolunteerBuilder(maxPerYear, years).apply(block).build()
    }

    internal fun build(): TranscriptGrading = TranscriptGrading(
        generalSubjects = requireNotNull(generalSubjects) { "[$type] generalSubjects 선언은 필수" },
        artsSubjects = requireNotNull(artsSubjects) { "[$type] artsSubjects 선언은 필수" },
        attendance = requireNotNull(attendance) { "[$type] attendance 선언은 필수" },
        volunteer = requireNotNull(volunteer) { "[$type] volunteer 선언은 필수" },
    )
}

@AdmissionDsl
class GeneralSubjectsBuilder internal constructor(private val max: Int) {
    private val achievementPoints = linkedMapOf<Achievement, Int>()
    private val semesterPoints = linkedMapOf<SemesterRef, BigDecimal>()
    private val fallback = mutableListOf<MissingSemesterStrategy>()

    fun achievement(vararg points: Pair<Achievement, Int>) {
        achievementPoints.putAll(points)
    }

    fun semester(year: Int, semester: Int, points: Int) {
        semesterPoints[SemesterRef(year, semester)] = points.toBigDecimal()
    }

    fun missingSemester(vararg strategies: MissingSemesterStrategy) {
        fallback += strategies
    }

    internal fun build(): GeneralSubjectRule = GeneralSubjectRule(
        maxScore = max.toBigDecimal(),
        achievementPoints = achievementPoints.toMap(),
        semesterPoints = semesterPoints.toMap(),
        missingSemesterFallback = fallback.toList(),
    )
}

@AdmissionDsl
class ArtsSubjectsBuilder internal constructor(private val max: Int) {
    private val achievementPoints = linkedMapOf<Achievement, Int>()

    fun achievement(vararg points: Pair<Achievement, Int>) {
        achievementPoints.putAll(points)
    }

    internal fun build(): ArtsSubjectRule = ArtsSubjectRule(
        maxScore = max.toBigDecimal(),
        achievementPoints = achievementPoints.toMap(),
    )
}

@AdmissionDsl
class AttendanceBuilder internal constructor(
    private val max: Int,
    private val years: List<Int>,
) {
    /** 미인정 지각·조퇴·결과 n회 = 결석 1일 */
    var latenessPerAbsenceDay: Int? = null

    /** 환산 결석 1일당 감점 */
    var deductionPerAbsenceDay: Int? = null

    /** 환산 결석일수가 이 값 이상이면 0점 */
    var zeroFromAbsenceDays: Int? = null

    /** 특정 학년 출결 자료가 없을 때 부여하는 기본점 */
    var missingYearDefault: Int? = null

    internal fun build(): AttendanceRule = AttendanceRule(
        years = years,
        maxScore = max.toBigDecimal(),
        latenessPerAbsenceDay = requireNotNull(latenessPerAbsenceDay) { "attendance.latenessPerAbsenceDay 선언은 필수" },
        deductionPerAbsenceDay = requireNotNull(deductionPerAbsenceDay) { "attendance.deductionPerAbsenceDay 선언은 필수" }.toBigDecimal(),
        zeroFromAbsenceDays = requireNotNull(zeroFromAbsenceDays) { "attendance.zeroFromAbsenceDays 선언은 필수" },
        missingYearDefault = requireNotNull(missingYearDefault) { "attendance.missingYearDefault 선언은 필수" }.toBigDecimal(),
    )
}

@AdmissionDsl
class VolunteerBuilder internal constructor(
    private val maxPerYear: Int,
    private val years: List<Int>,
) {
    private val steps = mutableListOf<VolunteerStep>()
    private var floorScore: Int? = null

    /** 특정 학년 봉사 자료가 없을 때 부여하는 기본점 */
    var missingYearDefault: Int? = null

    /** 연간 [minHours]시간 이상이면 [points]점 */
    fun step(minHours: Int, points: Int) {
        steps += VolunteerStep(minHours = minHours, points = points.toBigDecimal())
    }

    /** 모든 step 미달 시 점수 */
    fun floor(points: Int) {
        floorScore = points
    }

    internal fun build(): VolunteerRule = VolunteerRule(
        years = years,
        maxScorePerYear = maxPerYear.toBigDecimal(),
        steps = steps.toList(),
        floorScore = requireNotNull(floorScore) { "volunteer.floor 선언은 필수" }.toBigDecimal(),
        missingYearDefault = requireNotNull(missingYearDefault) { "volunteer.missingYearDefault 선언은 필수" }.toBigDecimal(),
    )
}

@AdmissionDsl
class FormulaGradingBuilder internal constructor(private val type: GraduationType) {
    private var subjectFormula: RangeScaleFormula? = null
    private var attendanceFixedScore: Int? = null
    private var volunteerFormula: RangeScaleFormula? = null

    /** 교과 = (입력 − minInput) ÷ (maxInput − minInput) × maxScore */
    fun subjects(minInput: Int, maxInput: Int, maxScore: Int) {
        subjectFormula = rangeScale(minInput, maxInput, maxScore)
    }

    fun attendanceFixed(score: Int) {
        attendanceFixedScore = score
    }

    /** 봉사 = (입력 − minInput) ÷ (maxInput − minInput) × maxScore */
    fun volunteer(minInput: Int, maxInput: Int, maxScore: Int) {
        volunteerFormula = rangeScale(minInput, maxInput, maxScore)
    }

    private fun rangeScale(minInput: Int, maxInput: Int, maxScore: Int) = RangeScaleFormula(
        minInput = minInput.toBigDecimal(),
        maxInput = maxInput.toBigDecimal(),
        maxScore = maxScore.toBigDecimal(),
    )

    internal fun build(): FormulaGrading = FormulaGrading(
        subjectFormula = requireNotNull(subjectFormula) { "[$type] subjects 환산식 선언은 필수" },
        attendanceFixedScore = requireNotNull(attendanceFixedScore) { "[$type] attendanceFixed 선언은 필수" }.toBigDecimal(),
        volunteerFormula = requireNotNull(volunteerFormula) { "[$type] volunteer 환산식 선언은 필수" },
    )
}
