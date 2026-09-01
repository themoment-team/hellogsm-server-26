package kr.hellogsm.entrance.dsl

import kr.hellogsm.entrance.plan.AbsentPolicy
import kr.hellogsm.entrance.plan.Round
import kr.hellogsm.entrance.plan.ScoreComponent
import kr.hellogsm.entrance.plan.ScoreComposition
import kr.hellogsm.entrance.plan.SelectionRule
import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plan.Tiebreaker
import java.math.BigDecimal

@AdmissionDsl
class RoundsBuilder internal constructor(private val rounds: MutableList<Round>) {
    fun round(code: String, name: String, block: RoundBuilder.() -> Unit) {
        rounds += RoundBuilder(code, name).apply(block).build()
    }
}

/** 점수 구성요소 참조 헬퍼. 점수를 조립하는 빌더들이 공유한다 */
@AdmissionDsl
sealed class ScoreComponentScope {
    /** 교과 성적 (일반교과 + 예체능) */
    val subjectScore: ScoreComponent get() = ScoreComponent.SubjectScore

    /** 출석 성적 */
    val attendanceScore: ScoreComponent get() = ScoreComponent.AttendanceScore

    /** 봉사활동 성적 */
    val volunteerScore: ScoreComponent get() = ScoreComponent.VolunteerScore

    /** 이전 차수의 총점 참조 */
    fun roundScore(roundCode: String): ScoreComponent = ScoreComponent.RoundScore(roundCode)

    /** 운영자가 수동 입력하는 점수 선언 (예: 역량검사, 심층면접) */
    fun manualScore(code: String, name: String, max: Int): ScoreComponent.ManualScore =
        ScoreComponent.ManualScore(code = code, name = name, maxScore = max.toBigDecimal())
}

class RoundBuilder internal constructor(
    private val code: String,
    private val name: String,
) : ScoreComponentScope() {
    private var selection: SelectionRule? = null
    private var score: ScoreComposition? = null
    private var tiebreakers = listOf<Tiebreaker>()

    /** 학과 구분 없는 총원제(All-cut) 선발 여부 */
    var allCut: Boolean = true

    var absentPolicy: AbsentPolicy = AbsentPolicy.EXCLUDE

    /** 전형별 모집정원 × [value]배수 이내 선발 */
    fun selectByMultiplier(value: Double) {
        selection = SelectionRule.Multiplier(BigDecimal.valueOf(value))
    }

    /** 전형별 모집정원만큼 선발 */
    fun selectByCapacity() {
        selection = SelectionRule.Capacity
    }

    /** 구성요소 단순 합산 점수 */
    fun sumScore(vararg components: ScoreComponent) {
        score = ScoreComposition.Sum(components.toList())
    }

    /** 가중 합산 점수 (반영 비율 합 = 100%) */
    fun weightedScore(max: Int, block: WeightedScoreBuilder.() -> Unit) {
        score = WeightedScoreBuilder(max).apply(block).build()
    }

    /** 동점자 처리 기준, 선언 순서 = 우선순위 */
    fun tiebreak(block: TiebreakBuilder.() -> Unit) {
        tiebreakers = TiebreakBuilder().apply(block).build()
    }

    internal fun build(): Round = Round(
        code = code,
        name = name,
        selection = requireNotNull(selection) { "차수 '$code': selectByMultiplier/selectByCapacity 선언은 필수" },
        allCut = allCut,
        score = requireNotNull(score) { "차수 '$code': sumScore/weightedScore 선언은 필수" },
        absentPolicy = absentPolicy,
        tiebreakers = tiebreakers,
    )
}

class WeightedScoreBuilder internal constructor(private val max: Int) : ScoreComponentScope() {
    private val parts = mutableListOf<ScoreComposition.Weighted.Part>()

    fun part(component: ScoreComponent, weightPercent: Int, normalizeTo: Int? = null) {
        parts += ScoreComposition.Weighted.Part(
            component = component,
            weightPercent = weightPercent.toBigDecimal(),
            normalizeTo = normalizeTo?.toBigDecimal(),
        )
    }

    internal fun build(): ScoreComposition.Weighted = ScoreComposition.Weighted(
        maxScore = max.toBigDecimal(),
        parts = parts.toList(),
    )
}

@AdmissionDsl
class TiebreakBuilder internal constructor() {
    private val tiebreakers = mutableListOf<Tiebreaker>()

    /** 예체능 제외 일반교과 성적이 우수한 자 */
    fun byGeneralSubjectScore() {
        tiebreakers += Tiebreaker.GeneralSubjectScore
    }

    /** 지정된 학기 순서대로 학기 성적 비교 (예: `bySemesters(3 to 1, 2 to 2)`) */
    fun bySemesters(vararg semesters: Pair<Int, Int>) {
        tiebreakers += Tiebreaker.SemesterScores(semesters.map { (year, semester) -> SemesterRef(year, semester) })
    }

    /** 비교과(출석 + 봉사) 성적이 우수한 자 */
    fun byNonSubjectScore() {
        tiebreakers += Tiebreaker.NonSubjectScore
    }

    /** 수동 입력 점수가 우수한 자 */
    fun byManualScore(code: String) {
        tiebreakers += Tiebreaker.ManualScore(code)
    }

    internal fun build(): List<Tiebreaker> = tiebreakers.toList()
}
