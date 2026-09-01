package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.engine.scoring.ScoreBreakdown
import kr.hellogsm.entrance.engine.scoring.TranscriptDetail
import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.SemesterRef
import java.math.BigDecimal

/**
 * evaluation·assignment 테스트용 지원자 생성 헬퍼.
 * 점수 구성: 총점 = 교과(subjects) + 출석 30 + 봉사 30. 교과 = 일반교과(general) + 예체능.
 */
internal fun testApplicant(
    id: String,
    screening: String,
    total: String,
    general: String? = null,
    semesters: Map<SemesterRef, String> = emptyMap(),
    nonSubjects: String = "60.000",
    graduationType: GraduationType = GraduationType.CANDIDATE,
    manualScores: Map<String, String> = emptyMap(),
    previousRoundScores: Map<String, String> = emptyMap(),
): RoundApplicant {
    val totalScore = BigDecimal(total)
    val nonSubjectsScore = BigDecimal(nonSubjects)
    val subjects = totalScore - nonSubjectsScore
    val generalScore = general?.let(::BigDecimal) ?: (subjects - BigDecimal(60))

    val detail =
        if (graduationType == GraduationType.GED) null
        else TranscriptDetail(
            generalSubjectsScore = generalScore,
            artsSubjectsScore = subjects - generalScore,
            semesterScores = semesters.mapValues { BigDecimal(it.value) },
            semesterSources = emptyMap(),
            attendanceDefaultedYears = emptyList(),
            volunteerDefaultedYears = emptyList(),
        )

    return RoundApplicant(
        id = id,
        screening = screening,
        breakdown = ScoreBreakdown(
            graduationType = graduationType,
            subjectsScore = subjects,
            attendanceScore = BigDecimal("30.000"),
            volunteerScore = nonSubjectsScore - BigDecimal("30.000"),
            nonSubjectsScore = nonSubjectsScore,
            totalScore = totalScore,
            transcriptDetail = detail,
        ),
        manualScores = manualScores.mapValues { BigDecimal(it.value) },
        previousRoundScores = previousRoundScores.mapValues { BigDecimal(it.value) },
    )
}

/** 총점이 [startTotal]부터 0.001씩 낮아지는 지원자 [count]명 */
internal fun rankedApplicants(
    prefix: String,
    screening: String,
    count: Int,
    startTotal: BigDecimal = BigDecimal("299.999"),
): List<RoundApplicant> = (1..count).map { i ->
    testApplicant(
        id = "$prefix$i",
        screening = screening,
        total = startTotal.subtract(BigDecimal("0.001").multiply(BigDecimal(i - 1))).toPlainString(),
    )
}
