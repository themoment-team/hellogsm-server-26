package kr.hellogsm.entrance.engine.scoring

import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.MissingSemesterStrategy
import kr.hellogsm.entrance.plan.SemesterRef
import java.math.BigDecimal

/**
 * 성적 계산 결과와 산출 근거(breakdown).
 *
 * 이의 제기 대응과 admin 노출을 위해 최종 총점뿐 아니라 영역별 점수와
 * 결측 처리 내역(대체 학기, 기본점 부여 학년)을 함께 담는다.
 * 모든 점수는 plan의 결과값 반올림 정책(scale 3)이 적용된 값이다.
 */
data class ScoreBreakdown(
    val graduationType: GraduationType,
    /** 교과 성적 = 일반교과 + 예체능 (2026 기준 만점 240) */
    val subjectsScore: BigDecimal,
    /** 출석 성적 (만점 30) */
    val attendanceScore: BigDecimal,
    /** 봉사활동 성적 (만점 30) */
    val volunteerScore: BigDecimal,
    /** 비교과 성적 = 출석 + 봉사 (만점 60) */
    val nonSubjectsScore: BigDecimal,
    /** 총점 = 교과 + 비교과 (만점 300) */
    val totalScore: BigDecimal,
    /** 내신 산출 상세. 검정고시(수식 산출)는 null */
    val transcriptDetail: TranscriptDetail?,
)

/** 내신 기반 산출(졸업예정자/졸업자)의 상세 근거 */
data class TranscriptDetail(
    /** 일반교과 성적 (만점 180) — 동점자 처리 기준 '교과 성적(예체능 미포함)' */
    val generalSubjectsScore: BigDecimal,
    /** 예체능 교과 성적 (만점 60) */
    val artsSubjectsScore: BigDecimal,
    /** 학기별 일반교과 환산점수 (plan 선언 순서) — 동점자 처리의 학기 비교 기준 */
    val semesterScores: Map<SemesterRef, BigDecimal>,
    /** 학기별 성적 출처 — 결측 학기 대체 적용 여부의 감사 기록 */
    val semesterSources: Map<SemesterRef, SemesterSource>,
    /** 출결 자료가 없어 기본점이 부여된 학년 */
    val attendanceDefaultedYears: List<Int>,
    /** 봉사 자료가 없어 기본점이 부여된 학년 */
    val volunteerDefaultedYears: List<Int>,
)

/** 학기 성적의 출처 */
sealed interface SemesterSource {
    /** 지원자가 제출한 해당 학기 성적을 그대로 사용 */
    data object Submitted : SemesterSource

    /** 결측 학기 대체 규칙으로 [from] 학기의 성적을 대신 사용 */
    data class Substituted(
        val from: SemesterRef,
        val strategy: MissingSemesterStrategy,
    ) : SemesterSource
}

/** 성적 산출이 불가능한 입력에 대한 오류 (예: 대체 불가능한 결측 학기, 정의되지 않은 성취도) */
class ScoringException(message: String) : IllegalArgumentException(message)
