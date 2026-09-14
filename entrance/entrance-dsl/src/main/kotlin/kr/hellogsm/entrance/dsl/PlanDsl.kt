package kr.hellogsm.entrance.dsl

import kr.hellogsm.entrance.plan.AdditionalRecruitmentPolicy
import kr.hellogsm.entrance.plan.AdmissionPlan
import kr.hellogsm.entrance.plan.Grading
import kr.hellogsm.entrance.plan.Major
import kr.hellogsm.entrance.plan.MajorAssignmentPolicy
import kr.hellogsm.entrance.plan.Quota
import kr.hellogsm.entrance.plan.Round
import kr.hellogsm.entrance.plan.ScheduleEvent
import kr.hellogsm.entrance.plan.Screening
import kr.hellogsm.entrance.plan.ScreeningSubType
import kr.hellogsm.entrance.plan.WaitlistPolicy
import java.time.LocalDate

@DslMarker
annotation class AdmissionDsl

/**
 * 입학전형 요강 선언의 진입점.
 *
 * ```
 * val plan = admissionPlan(year = 2026) {
 *     majors { ... }
 *     screenings { ... }
 *     grading { ... }
 *     rounds { ... }
 *     majorAssignment { ... }
 * }
 * ```
 */
fun admissionPlan(year: Int, block: AdmissionPlanBuilder.() -> Unit): AdmissionPlan =
    AdmissionPlanBuilder(year).apply(block).build()

@AdmissionDsl
class AdmissionPlanBuilder internal constructor(private val year: Int) {
    private val majors = mutableListOf<Major>()
    private val screenings = mutableListOf<Screening>()
    private var grading: Grading? = null
    private val rounds = mutableListOf<Round>()
    private var majorAssignment: MajorAssignmentPolicy? = null
    private var waitlist: WaitlistPolicy? = null
    private var additionalRecruitment: AdditionalRecruitmentPolicy? = null
    private val schedule = mutableListOf<ScheduleEvent>()

    fun majors(block: MajorsBuilder.() -> Unit) {
        MajorsBuilder(majors).block()
    }

    fun screenings(block: ScreeningsBuilder.() -> Unit) {
        ScreeningsBuilder(screenings).block()
    }

    fun grading(block: GradingBuilder.() -> Unit) {
        grading = GradingBuilder().apply(block).build()
    }

    fun rounds(block: RoundsBuilder.() -> Unit) {
        RoundsBuilder(rounds).block()
    }

    fun majorAssignment(block: MajorAssignmentBuilder.() -> Unit) {
        majorAssignment = MajorAssignmentBuilder().apply(block).build()
    }

    fun waitlist(block: WaitlistBuilder.() -> Unit) {
        waitlist = WaitlistBuilder().apply(block).build()
    }

    fun additionalRecruitment(block: AdditionalRecruitmentBuilder.() -> Unit) {
        additionalRecruitment = AdditionalRecruitmentBuilder().apply(block).build()
    }

    fun schedule(block: ScheduleBuilder.() -> Unit) {
        ScheduleBuilder(schedule).block()
    }

    internal fun build(): AdmissionPlan = AdmissionPlan(
        year = year,
        majors = majors.toList(),
        screenings = screenings.toList(),
        grading = requireNotNull(grading) { "grading 블록은 필수" },
        rounds = rounds.toList(),
        majorAssignment = requireNotNull(majorAssignment) { "majorAssignment 블록은 필수" },
        waitlist = waitlist,
        additionalRecruitment = additionalRecruitment,
        schedule = schedule.toList(),
    )
}

@AdmissionDsl
class MajorsBuilder internal constructor(private val majors: MutableList<Major>) {
    fun major(code: String, name: String, capacity: Int) {
        majors += Major(code = code, name = name, capacity = capacity)
    }
}

@AdmissionDsl
class ScreeningsBuilder internal constructor(private val screenings: MutableList<Screening>) {
    /** 정원 내 전형. quota를 선언하지 않으면 Remainder(나머지 전부) */
    fun regular(code: String, name: String, block: RegularScreeningBuilder.() -> Unit = {}) {
        screenings += RegularScreeningBuilder(code, name).apply(block).build()
    }

    /** 정원 외 전형. quota 선언 필수 */
    fun extra(code: String, name: String, block: ExtraScreeningBuilder.() -> Unit) {
        screenings += ExtraScreeningBuilder(code, name).apply(block).build()
    }
}

@AdmissionDsl
class RegularScreeningBuilder internal constructor(
    private val code: String,
    private val name: String,
) {
    private var quota: Quota = Quota.Remainder
    private var unfilledGoesTo: String? = null
    private var rejectedFallsTo: String? = null
    private val subTypes = mutableListOf<ScreeningSubType>()

    fun quota(count: Int) {
        quota = Quota.Fixed(count)
    }

    fun unfilledGoesTo(screeningCode: String) {
        unfilledGoesTo = screeningCode
    }

    fun rejectedFallsTo(screeningCode: String) {
        rejectedFallsTo = screeningCode
    }

    fun subType(code: String, name: String) {
        subTypes += ScreeningSubType(code = code, name = name)
    }

    internal fun build(): Screening = Screening(
        code = code,
        name = name,
        withinCapacity = true,
        quota = quota,
        unfilledGoesTo = unfilledGoesTo,
        rejectedFallsTo = rejectedFallsTo,
        subTypes = subTypes.toList(),
    )
}

@AdmissionDsl
class ExtraScreeningBuilder internal constructor(
    private val code: String,
    private val name: String,
) {
    private var quota: Quota.Fixed? = null
    private var admitOnlyWithinFirstRoundCutline = false
    private var overflowFallsTo: String? = null
    private val subTypes = mutableListOf<ScreeningSubType>()

    fun quota(count: Int, capPercentOfTotal: Int? = null) {
        quota = Quota.Fixed(count = count, capPercentOfTotal = capPercentOfTotal?.toBigDecimal())
    }

    /** 1차(정원 내) 합격자 최저점 이내인 경우에만 정원 외로 전형 */
    fun admitOnlyWithinFirstRoundCutline() {
        admitOnlyWithinFirstRoundCutline = true
    }

    /** 정원 외 모집범위 초과 시 편입되는 전형 */
    fun overflowFallsTo(screeningCode: String) {
        overflowFallsTo = screeningCode
    }

    fun subType(code: String, name: String) {
        subTypes += ScreeningSubType(code = code, name = name)
    }

    internal fun build(): Screening = Screening(
        code = code,
        name = name,
        withinCapacity = false,
        quota = requireNotNull(quota) { "정원 외 전형 '$code'에는 quota 선언이 필수" },
        admitOnlyWithinFirstRoundCutline = admitOnlyWithinFirstRoundCutline,
        overflowFallsTo = overflowFallsTo,
        subTypes = subTypes.toList(),
    )
}

@AdmissionDsl
class MajorAssignmentBuilder internal constructor() {
    var choiceCount: Int? = null
    var extraScreeningCapPerMajor: Int? = null

    internal fun build(): MajorAssignmentPolicy = MajorAssignmentPolicy(
        choiceCount = requireNotNull(choiceCount) { "majorAssignment.choiceCount 선언은 필수" },
        extraScreeningCapPerMajor = extraScreeningCapPerMajor,
    )
}

@AdmissionDsl
class WaitlistBuilder internal constructor() {
    private var percent: Int? = null
    private var from: String? = null

    fun percentOfTotal(percent: Int) {
        this.percent = percent
    }

    fun from(screeningCode: String) {
        from = screeningCode
    }

    internal fun build(): WaitlistPolicy = WaitlistPolicy(
        percentOfTotalCapacity = requireNotNull(percent) { "waitlist.percentOfTotal 선언은 필수" }.toBigDecimal(),
        fromScreening = requireNotNull(from) { "waitlist.from 선언은 필수" },
    )
}

@AdmissionDsl
class AdditionalRecruitmentBuilder internal constructor() {
    private val screenings = mutableListOf<String>()
    private var basedOnRound: String? = null

    fun screening(code: String) {
        screenings += code
    }

    fun basedOnRound(roundCode: String) {
        basedOnRound = roundCode
    }

    internal fun build(): AdditionalRecruitmentPolicy = AdditionalRecruitmentPolicy(
        screenings = screenings.toList(),
        basedOnRound = requireNotNull(basedOnRound) { "additionalRecruitment.basedOnRound 선언은 필수" },
    )
}

@AdmissionDsl
class ScheduleBuilder internal constructor(private val events: MutableList<ScheduleEvent>) {
    fun event(code: String, name: String, start: LocalDate, end: LocalDate = start) {
        events += ScheduleEvent(code = code, name = name, start = start, end = end)
    }
}
