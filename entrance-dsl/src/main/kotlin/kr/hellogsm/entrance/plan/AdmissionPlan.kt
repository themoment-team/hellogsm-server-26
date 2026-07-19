package kr.hellogsm.entrance.plan

import java.time.LocalDate

/**
 * 한 학년도 입학전형 요강 전체를 표현하는 불변 모델.
 *
 * DSL 빌더(`admissionPlan { ... }`)의 산출물이며, 엔진은 이 모델만 해석한다.
 * 생성 시점에 [PlanValidator]가 정합성을 검증하므로, 존재하는 인스턴스는 항상 유효하다.
 */
data class AdmissionPlan(
    val year: Int,
    val majors: List<Major>,
    val screenings: List<Screening>,
    val grading: Grading,
    val rounds: List<Round>,
    val majorAssignment: MajorAssignmentPolicy,
    val waitlist: WaitlistPolicy?,
    val additionalRecruitment: AdditionalRecruitmentPolicy?,
    val schedule: List<ScheduleEvent>,
) {
    /** 정원 내 총 모집정원 (학과별 정원의 합) */
    val totalCapacity: Int = majors.sumOf(Major::capacity)

    init {
        PlanValidator.validate(this)
    }

    fun major(code: String): Major =
        majors.firstOrNull { it.code == code } ?: error("존재하지 않는 학과 코드: $code")

    fun screening(code: String): Screening =
        screenings.firstOrNull { it.code == code } ?: error("존재하지 않는 전형 코드: $code")

    fun round(code: String): Round =
        rounds.firstOrNull { it.code == code } ?: error("존재하지 않는 전형 차수 코드: $code")

    /**
     * 전형의 실제 모집인원. [Quota.Remainder]는 정원 내 고정 정원을 제외한 나머지로 해석한다.
     */
    fun resolvedQuota(screeningCode: String): Int =
        when (val quota = screening(screeningCode).quota) {
            is Quota.Fixed -> quota.count
            Quota.Remainder -> totalCapacity - screenings
                .filter { it.withinCapacity }
                .map(Screening::quota)
                .filterIsInstance<Quota.Fixed>()
                .sumOf(Quota.Fixed::count)
        }
}

/** 학과 */
data class Major(
    val code: String,
    val name: String,
    val capacity: Int,
)

/** 전형 일정 항목 (MVP에서는 참조 데이터, UI 단계에서 상태 전이에 활용) */
data class ScheduleEvent(
    val code: String,
    val name: String,
    val start: LocalDate,
    val end: LocalDate,
)
