package kr.hellogsm.entrance.lambda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.math.BigDecimal

/**
 * server의 `LambdaScoreCalculatorReqDto`와 JSON 필드명이 1:1로 대응해야 한다 —
 * 이 계약이 깨지면 server 코드 변경 없이는 소비처를 전환할 수 없다.
 *
 * `achievement1_1`은 plan이 직접 채점하는 학기는 아니지만(1-2부터 반영), 결측 학기
 * 대체(`MissingSemesterStrategy.SAME_YEAR_OTHER_SEMESTER`)의 원본으로 쓰일 수 있어
 * `StudentRecordMapper`에 그대로 전달한다 — 최종 성적 계산 시점(`ScoringEngine`)에서
 * plan에 선언된 대체 규칙이 항상 적용되어야 하기 때문이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ScoreCalculatorRequest(
    val achievement1_1: List<Int>? = null,
    val achievement1_2: List<Int>? = null,
    val achievement2_1: List<Int>? = null,
    val achievement2_2: List<Int>? = null,
    val achievement3_1: List<Int>? = null,
    val achievement3_2: List<Int>? = null,
    val generalSubjects: List<String>? = null,
    val newSubjects: List<String>? = null,
    val artsPhysicalAchievement: List<Int>? = null,
    val artsPhysicalSubjects: List<String>? = null,
    val absentDays: List<Int>? = null,
    val attendanceDays: List<Int>? = null,
    val volunteerTime: List<Int>? = null,
    val liberalSystem: String? = null,
    val freeSemester: String? = null,
    val gedAvgScore: BigDecimal? = null,
    val graduationType: String? = null,
)
