package kr.hellogsm.entrance.batch.mapping

import kr.hellogsm.entrance.engine.scoring.ScoringEngine
import kr.hellogsm.entrance.plans.plan2026
import team.themoment.hellogsmv3.domain.oneseo.entity.MiddleSchoolAchievement
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `StudentRecordMapper` 가 go-hellogsm-score-calculator(d7b65b4)와 동일한 입력 해석을 하는지,
 * 매핑 결과를 `ScoringEngine` 에 넣어 산출한 점수로 검증한다.
 *
 * 케이스는 엔진 scoring golden(GoParityGoldenCases)의 CANDIDATE-001 과 동일한 원본 성적이며,
 * 그 원본을 `MiddleSchoolAchievement` 엔티티(=DB 표현)로 재구성해 매퍼를 거친다.
 * (출결: 지각·조퇴·결과가 latenessCount 로 합쳐져도 학년 합산 후 ÷3 이므로 점수 동일)
 */
class StudentRecordMapperTest {

    private val scoring = ScoringEngine(plan2026)

    @Test
    fun `MiddleSchoolAchievement 를 매핑해 채점하면 Go parity golden 점수와 일치한다`() {
        val achievement = MiddleSchoolAchievement.builder()
            .achievement1_2(listOf(3, 3, 3, 4, 5, 3, 1, 4, 2, 1))
            .achievement2_1(listOf(1, 3, 3, 1, 3, 5, 3))
            .achievement2_2(listOf(3, 4, 3, 1, 1, 5, 1))
            .achievement3_1(listOf(4, 5, 2, 4, 1, 3, 3, 5, 3, 1, 2))
            .achievement3_2(emptyList()) // 졸업예정자는 3-2 미반영
            .artsPhysicalAchievement(listOf(3, 3, 4, 5))
            // 출결: absentDays[학년], attendanceDays = 학년별 (지각,조퇴,결과)×3
            .absentDays(listOf(4, 1, 2))
            .attendanceDays(listOf(0, 3, 2, 4, 1, 2, 4, 0, 1))
            .volunteerTime(listOf(5, 2, 2))
            .build()

        val record = StudentRecordMapper.toStudentRecord(achievement, GraduationType.CANDIDATE)
        val score = scoring.score(record)

        assertEquals("156.212", score.totalScore.toPlainString(), "총점")
        assertEquals("146.212", score.subjectsScore.toPlainString(), "교과 총점")
        assertEquals("0.000", score.attendanceScore.toPlainString(), "출석 점수")
        assertEquals("10.000", score.volunteerScore.toPlainString(), "봉사 점수")
        assertEquals("101.212", score.transcriptDetail!!.generalSubjectsScore.toPlainString(), "일반교과")
        assertEquals("45.000", score.transcriptDetail!!.artsSubjectsScore.toPlainString(), "예체능")
    }

    @Test
    fun `검정고시는 gedAvgScore 로 StudentRecord_Ged 를 만든다`() {
        val achievement = MiddleSchoolAchievement.builder()
            .gedAvgScore(BigDecimal("92.50"))
            .build()

        val record = StudentRecordMapper.toStudentRecord(achievement, GraduationType.GED)

        // 검정고시는 수식 기반 산출 — 매핑이 GED 경로를 타는지와 채점 가능 여부만 확인
        val score = scoring.score(record)
        assertEquals(GraduationType.GED.name, score.graduationType.name, "졸업 구분")
    }
}
