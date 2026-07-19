package kr.hellogsm.entrance.plan

import java.math.BigDecimal

/** 최종 학과 배정 규칙 */
data class MajorAssignmentPolicy(
    /** 지망 학과 수 (전부 기재 필수) */
    val choiceCount: Int,
    /** 정원 외 합격자의 학과당 최대 배정 인원 (null이면 상한 없음) */
    val extraScreeningCapPerMajor: Int?,
)

/** 예비 합격자 선정 규칙 */
data class WaitlistPolicy(
    /** 모집정원 대비 발표 범위(%) */
    val percentOfTotalCapacity: BigDecimal,
    /** 예비합격자를 선발할 전형 (불합격자 중 고득점 순) */
    val fromScreening: String,
)

/** 추가모집(모집정원 미달 시) 규칙 */
data class AdditionalRecruitmentPolicy(
    /** 추가모집을 진행하는 전형 (예: 일반전형만) */
    val screenings: List<String>,
    /** 선발 점수 기준이 되는 차수 (예: 1차 환산점수만으로 선발) */
    val basedOnRound: String,
)
