package kr.hellogsm.entrance.plan

import java.math.BigDecimal
import java.math.RoundingMode

/** 졸업 구분 */
enum class GraduationType {
    /** 졸업예정자 */
    CANDIDATE,

    /** 졸업자 */
    GRADUATE,

    /** 검정고시 합격자 */
    GED,
}

/** 성취도 (5등급 평가: A~E, 예체능 3등급 평가: A~C) */
enum class Achievement { A, B, C, D, E }

/** 학년-학기 참조 (예: 3학년 1학기 = SemesterRef(3, 1)) */
data class SemesterRef(val year: Int, val semester: Int) {
    init {
        require(year in 1..3) { "학년은 1..3 범위여야 함: $year" }
        require(semester in 1..2) { "학기는 1..2 범위여야 함: $semester" }
    }

    override fun toString(): String = "$year-$semester"
}

/**
 * 요강에 명시된 반올림 정책. scale은 반올림 후 남기는 소수점 자릿수.
 * (예: 2026 요강 — 중간값은 소수점 여섯째 자리에서 반올림해 다섯째 자리까지(scale 5),
 * 결과값은 소수점 넷째 자리에서 반올림(scale 3))
 */
data class RoundingPolicy(
    val intermediateScale: Int,
    val intermediateMode: RoundingMode,
    val resultScale: Int,
    val resultMode: RoundingMode,
)

/** 졸업 구분별 성적 산출 스키마 모음 */
data class Grading(
    val rounding: RoundingPolicy,
    val schemes: Map<GraduationType, GradingScheme>,
) {
    fun scheme(type: GraduationType): GradingScheme =
        schemes[type] ?: error("성적 산출 스키마가 정의되지 않은 졸업 구분: $type")

    /**
     * 성적 총점 만점 (예: 2026 = 300). 모든 졸업 구분의 만점이 같아야 하며([PlanValidator] 검증),
     * 차수 점수 정규화(예: 1차 300점 → 100점)의 분모로 쓰인다.
     */
    val totalMaxScore: BigDecimal
        get() = schemes.values.first().maxScore
}

sealed interface GradingScheme {
    /** 이 스키마의 총점 만점 */
    val maxScore: BigDecimal
}

/** 내신 기반 산출 (졸업예정자/졸업자) */
data class TranscriptGrading(
    val generalSubjects: GeneralSubjectRule,
    val artsSubjects: ArtsSubjectRule,
    val attendance: AttendanceRule,
    val volunteer: VolunteerRule,
) : GradingScheme {
    override val maxScore: BigDecimal =
        generalSubjects.maxScore + artsSubjects.maxScore + attendance.maxScore + volunteer.maxScore
}

/** 일반교과 산출 규칙 */
data class GeneralSubjectRule(
    val maxScore: BigDecimal,
    /** 성취도 → 환산점수 (예: A=5 … E=1) */
    val achievementPoints: Map<Achievement, Int>,
    /** 학기별 배점. 합은 [maxScore]와 같아야 하며 선언 순서를 유지한다 */
    val semesterPoints: Map<SemesterRef, BigDecimal>,
    /** 결측 학기 대체 전략, 우선순위 순 */
    val missingSemesterFallback: List<MissingSemesterStrategy>,
)

enum class MissingSemesterStrategy {
    /** 같은 학년의 다른 학기 성적으로 대체 */
    SAME_YEAR_OTHER_SEMESTER,

    /** 차상위 학년을 기준으로 학기별 적용 */
    UPPER_YEAR,

    /** 차하위 학년을 기준으로 학기별 적용 */
    LOWER_YEAR,
}

/** 예체능(체육·음악·미술) 교과 산출 규칙: 3년간 성취도 환산점수 평균 × 만점 비율 */
data class ArtsSubjectRule(
    val maxScore: BigDecimal,
    /** 성취도 → 환산점수 (예: A=5, B=4, C=3) */
    val achievementPoints: Map<Achievement, Int>,
)

/** 출석 산출 규칙 (전 학년 출결 합산) */
data class AttendanceRule(
    /** 반영 학년 */
    val years: List<Int>,
    val maxScore: BigDecimal,
    /** 미인정 지각·조퇴·결과 n회 = 결석 1일 (소수점 버림) */
    val latenessPerAbsenceDay: Int,
    /** 환산 결석 1일당 감점 */
    val deductionPerAbsenceDay: BigDecimal,
    /** 환산 결석일수가 이 값 이상이면 0점 */
    val zeroFromAbsenceDays: Int,
    /** 특정 학년 출결 자료가 없을 때 부여하는 기본점 */
    val missingYearDefault: BigDecimal,
)

/** 봉사활동 산출 규칙 (학년별 계단 함수) */
data class VolunteerRule(
    /** 반영 학년 */
    val years: List<Int>,
    val maxScorePerYear: BigDecimal,
    /** 연간 시간 기준 계단. minHours 내림차순으로 첫 매칭 적용 */
    val steps: List<VolunteerStep>,
    /** 모든 step 미달 시 점수 */
    val floorScore: BigDecimal,
    /** 특정 학년 봉사 자료가 없을 때 부여하는 기본점 */
    val missingYearDefault: BigDecimal,
) {
    val maxScore: BigDecimal = maxScorePerYear * years.size.toBigDecimal()
}

data class VolunteerStep(
    val minHours: Int,
    val points: BigDecimal,
)

/**
 * 수식 기반 산출 (검정고시).
 * score = (input - minInput) / (maxInput - minInput) × maxScore
 */
data class RangeScaleFormula(
    val minInput: BigDecimal,
    val maxInput: BigDecimal,
    val maxScore: BigDecimal,
)

/** 검정고시 성적 산출 스키마 */
data class FormulaGrading(
    /** 교과 환산식 (예: (평균점수 − 60) ÷ 40 × 240) */
    val subjectFormula: RangeScaleFormula,
    /** 출석 고정 점수 */
    val attendanceFixedScore: BigDecimal,
    /** 봉사활동 환산식 */
    val volunteerFormula: RangeScaleFormula,
) : GradingScheme {
    override val maxScore: BigDecimal =
        subjectFormula.maxScore + attendanceFixedScore + volunteerFormula.maxScore
}
