package kr.hellogsm.entrance.batch.pipeline

import kr.hellogsm.entrance.batch.mapping.CodeMapping
import kr.hellogsm.entrance.batch.mapping.StudentRecordMapper
import kr.hellogsm.entrance.batch.persistence.BatchOneseoRepository
import kr.hellogsm.entrance.engine.evaluation.RoundApplicant
import kr.hellogsm.entrance.engine.scoring.ScoringEngine
import org.springframework.stereotype.Component
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo
import java.math.BigDecimal

/**
 * DB 로딩 결과 한 건 — 결과 write-back 을 위해 원본 엔티티 핸들을 함께 들고 있는다.
 * [recomputedTotal] 은 재계산 총점(저장값 대조 검증용).
 */
data class LoadedApplicant(
    val oneseo: Oneseo,
    val testResult: EntranceTestResult,
    val applicant: RoundApplicant,
    val choices: List<String>,
    val recomputedTotal: BigDecimal,
)

/**
 * 접수 완료 지원자를 읽어 `ScoringEngine` 으로 재채점하고 `RoundApplicant`(1차 시작 = 지망 전형)로
 * 변환한다. 역량검사·심층면접 점수는 [RoundApplicant.manualScores]("COMPETENCY"/"INTERVIEW")로,
 * 지망 학과는 지망 순서대로 코드 리스트로 싣는다.
 */
@Component
class ApplicantLoader(
    private val oneseoRepository: BatchOneseoRepository,
    private val scoringEngine: ScoringEngine,
) {

    fun load(): List<LoadedApplicant> =
        oneseoRepository.findAllByRealOneseoArrivedYn(YesNo.YES).mapNotNull(::toLoaded)

    private fun toLoaded(oneseo: Oneseo): LoadedApplicant? {
        val achievement = oneseo.middleSchoolAchievement ?: return null
        val privacy = oneseo.oneseoPrivacyDetail ?: return null
        val testResult = oneseo.entranceTestResult ?: return null

        val record = StudentRecordMapper.toStudentRecord(achievement, privacy.graduationType)
        val breakdown = scoringEngine.score(record)

        val manualScores = buildMap {
            testResult.competencyEvaluationScore?.let { put("COMPETENCY", it) }
            testResult.interviewScore?.let { put("INTERVIEW", it) }
        }

        val applicant = RoundApplicant(
            id = oneseo.id.toString(),
            screening = CodeMapping.screeningCode(oneseo.wantedScreening),
            breakdown = breakdown,
            manualScores = manualScores,
        )

        val choices = with(oneseo.desiredMajors) {
            listOf(firstDesiredMajor, secondDesiredMajor, thirdDesiredMajor)
        }.map(CodeMapping::majorCode)

        return LoadedApplicant(oneseo, testResult, applicant, choices, breakdown.totalScore)
    }
}
