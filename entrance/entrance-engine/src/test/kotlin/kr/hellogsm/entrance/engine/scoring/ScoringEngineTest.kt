package kr.hellogsm.entrance.engine.scoring

import kr.hellogsm.entrance.plan.Achievement
import kr.hellogsm.entrance.plan.Achievement.A
import kr.hellogsm.entrance.plan.Achievement.B
import kr.hellogsm.entrance.plan.Achievement.C
import kr.hellogsm.entrance.plan.Achievement.D
import kr.hellogsm.entrance.plan.Achievement.E
import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.GraduationType.CANDIDATE
import kr.hellogsm.entrance.plan.GraduationType.GRADUATE
import kr.hellogsm.entrance.plan.MissingSemesterStrategy
import kr.hellogsm.entrance.plan.SemesterRef
import kr.hellogsm.entrance.plans.plan
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * 2026 요강(부록 3, 성적 산출지침)의 수치를 고정하는 scoring 엔진 테스트.
 * 기대값은 요강의 환산식으로 손으로 계산한 값이다.
 */
class ScoringEngineTest {

    private val engine = ScoringEngine(plan)

    // ── 테스트 헬퍼 ────────────────────────────────────────────────

    private fun semester(year: Int, sem: Int) = SemesterRef(year, sem)

    private fun fullMarks(count: Int = 5): List<Achievement> = List(count) { A }

    private val candidateSlots = listOf(semester(1, 2), semester(2, 1), semester(2, 2), semester(3, 1))
    private val graduateSlots = listOf(semester(2, 1), semester(2, 2), semester(3, 1), semester(3, 2))

    private fun cleanAttendance() = AttendanceRecord(0, 0, 0, 0)

    private fun transcript(
        type: GraduationType = CANDIDATE,
        semesters: Map<SemesterRef, List<Achievement>> = candidateSlots.associateWith { fullMarks() },
        arts: List<Achievement> = List(6) { A },
        attendance: Map<Int, AttendanceRecord> = (1..3).associateWith { cleanAttendance() },
        volunteer: Map<Int, Int> = mapOf(1 to 7, 2 to 7, 3 to 7),
    ) = StudentRecord.Transcript(type, semesters, arts, attendance, volunteer)

    // ── 만점과 학기 배점 ───────────────────────────────────────────

    @Test
    fun `졸업예정자 전 영역 만점이면 총점 300이다`() {
        val breakdown = engine.score(transcript())

        assertEquals(BigDecimal("300.000"), breakdown.totalScore)
        assertEquals(BigDecimal("240.000"), breakdown.subjectsScore)
        assertEquals(BigDecimal("60.000"), breakdown.nonSubjectsScore)
        assertEquals(BigDecimal("30.000"), breakdown.attendanceScore)
        assertEquals(BigDecimal("30.000"), breakdown.volunteerScore)

        val detail = breakdown.transcriptDetail!!
        assertEquals(BigDecimal("180.000"), detail.generalSubjectsScore)
        assertEquals(BigDecimal("60.000"), detail.artsSubjectsScore)
        assertEquals(
            mapOf(
                semester(1, 2) to BigDecimal("18.000"),
                semester(2, 1) to BigDecimal("45.000"),
                semester(2, 2) to BigDecimal("45.000"),
                semester(3, 1) to BigDecimal("72.000"),
            ),
            detail.semesterScores,
        )
    }

    @Test
    fun `졸업자 전 영역 만점이면 학기 배점 36-36-54-54로 총점 300이다`() {
        val breakdown = engine.score(
            transcript(type = GRADUATE, semesters = graduateSlots.associateWith { fullMarks() }),
        )

        assertEquals(BigDecimal("300.000"), breakdown.totalScore)
        assertEquals(
            mapOf(
                semester(2, 1) to BigDecimal("36.000"),
                semester(2, 2) to BigDecimal("36.000"),
                semester(3, 1) to BigDecimal("54.000"),
                semester(3, 2) to BigDecimal("54.000"),
            ),
            breakdown.transcriptDetail!!.semesterScores,
        )
    }

    // ── 일반교과 학기별 산출 ───────────────────────────────────────

    @Test
    fun `학기 성적은 성취도 합을 과목수x5로 나눠 배점을 곱한다`() {
        // 1-2: A+B+C+D+E = 15, 15 ÷ 25 = 0.6 → × 18점 = 10.8
        val semesters = candidateSlots.associateWith { fullMarks() } +
            mapOf(semester(1, 2) to listOf(A, B, C, D, E))
        val detail = engine.score(transcript(semesters = semesters)).transcriptDetail!!

        assertEquals(BigDecimal("10.800"), detail.semesterScores.getValue(semester(1, 2)))
        // 일반교과 총점 = 10.8 + 45 + 45 + 72
        assertEquals(BigDecimal("172.800"), detail.generalSubjectsScore)
    }

    @Test
    fun `학기 성적의 나눗셈 몫은 소수점 다섯째 자리까지 반올림한 뒤 배점을 곱한다`() {
        // 3-1: 7과목 E×5 + D×2 = 9점, 9 ÷ 35 = 0.257142857… → 0.25714 → × 72 = 18.51408 → 18.514
        val semesters = candidateSlots.associateWith { fullMarks() } +
            mapOf(semester(3, 1) to listOf(E, E, E, E, E, D, D))
        val detail = engine.score(transcript(semesters = semesters)).transcriptDetail!!

        assertEquals(BigDecimal("18.514"), detail.semesterScores.getValue(semester(3, 1)))
    }

    // ── 예체능 교과 ───────────────────────────────────────────────

    @Test
    fun `예체능은 성취도 환산점수 평균점에 60을 곱한다`() {
        // A,A,B,B,C,C = 24점, 24 ÷ 30 = 0.8 → × 60 = 48
        val breakdown = engine.score(transcript(arts = listOf(A, A, B, B, C, C)))

        assertEquals(BigDecimal("48.000"), breakdown.transcriptDetail!!.artsSubjectsScore)
    }

    @Test
    fun `예체능 평균점은 소수점 넷째 자리에서 반올림한 뒤 60을 곱한다`() {
        // A×3 + B×2 + C×2 = 29점, 29 ÷ 35 = 0.82857… → 0.829 → × 60 = 49.74
        // (평균점을 먼저 반올림하지 않으면 49.714가 되므로 반올림 순서를 고정하는 테스트)
        val breakdown = engine.score(transcript(arts = listOf(A, A, A, B, B, C, C)))

        assertEquals(BigDecimal("49.740"), breakdown.transcriptDetail!!.artsSubjectsScore)
    }

    @Test
    fun `예체능에 D 성취도가 오면 산출 불가 오류다`() {
        assertFailsWith<ScoringException> {
            engine.score(transcript(arts = listOf(A, B, D)))
        }
    }

    // ── 출석 성적 (요강 표 3) ─────────────────────────────────────

    private fun attendanceOf(absence: Int, lateness: Int = 0) = mapOf(
        1 to AttendanceRecord(absence, lateness, 0, 0),
        2 to cleanAttendance(),
        3 to cleanAttendance(),
    )

    @Test
    fun `결석 1일마다 3점씩 감점한다`() {
        assertEquals(BigDecimal("30.000"), engine.score(transcript(attendance = attendanceOf(0))).attendanceScore)
        assertEquals(BigDecimal("27.000"), engine.score(transcript(attendance = attendanceOf(1))).attendanceScore)
        assertEquals(BigDecimal("24.000"), engine.score(transcript(attendance = attendanceOf(2))).attendanceScore)
        assertEquals(BigDecimal("3.000"), engine.score(transcript(attendance = attendanceOf(9))).attendanceScore)
    }

    @Test
    fun `환산 결석일수 10일 이상이면 출석은 0점이다`() {
        assertEquals(BigDecimal("0.000"), engine.score(transcript(attendance = attendanceOf(10))).attendanceScore)
        // 결석 9일 + 지각 3회(= 결석 1일) = 10일
        assertEquals(BigDecimal("0.000"), engine.score(transcript(attendance = attendanceOf(9, lateness = 3))).attendanceScore)
    }

    @Test
    fun `지각·조퇴·결과 3회는 결석 1일로 환산하고 소수점 이하는 버린다`() {
        // 지각 2 + 조퇴 2 + 결과 1 = 5회 → ⌊5÷3⌋ = 결석 1일 → 27점
        val attendance = mapOf(
            1 to AttendanceRecord(0, 2, 2, 1),
            2 to cleanAttendance(),
            3 to cleanAttendance(),
        )
        assertEquals(BigDecimal("27.000"), engine.score(transcript(attendance = attendance)).attendanceScore)
    }

    @Test
    fun `출결은 전 학년 합산으로 환산한다`() {
        // 학년별 지각 2회씩 = 6회 → 학년별 ⌊2÷3⌋=0이 아니라 합산 ⌊6÷3⌋ = 결석 2일 → 24점
        val attendance = (1..3).associateWith { AttendanceRecord(0, 2, 0, 0) }
        assertEquals(BigDecimal("24.000"), engine.score(transcript(attendance = attendance)).attendanceScore)
    }

    @Test
    fun `출결 자료가 없는 학년은 기본점 5점을 부여한다`() {
        // 1학년 결측: 기본점 5 + 남은 두 학년 몫 20(무결석) = 25
        val attendance = mapOf(2 to cleanAttendance(), 3 to cleanAttendance())
        val breakdown = engine.score(transcript(attendance = attendance))

        assertEquals(BigDecimal("25.000"), breakdown.attendanceScore)
        assertEquals(listOf(1), breakdown.transcriptDetail!!.attendanceDefaultedYears)
    }

    // ── 봉사활동 성적 (요강 표 4) ─────────────────────────────────

    @Test
    fun `봉사 계단 - 연 7시간 10점, 6시간 8점, 5시간 6점, 4시간 4점, 3시간 이하 2점`() {
        assertEquals(BigDecimal("30.000"), engine.score(transcript(volunteer = mapOf(1 to 7, 2 to 8, 3 to 100))).volunteerScore)
        assertEquals(BigDecimal("20.000"), engine.score(transcript(volunteer = mapOf(1 to 7, 2 to 6, 3 to 3))).volunteerScore)
        assertEquals(BigDecimal("12.000"), engine.score(transcript(volunteer = mapOf(1 to 5, 2 to 4, 3 to 0))).volunteerScore)
        assertEquals(BigDecimal("6.000"), engine.score(transcript(volunteer = mapOf(1 to 0, 2 to 1, 3 to 3))).volunteerScore)
    }

    @Test
    fun `봉사 자료가 없는 학년은 기본점 2점을 부여한다`() {
        val breakdown = engine.score(transcript(volunteer = mapOf(1 to 7, 2 to 7)))

        assertEquals(BigDecimal("22.000"), breakdown.volunteerScore)
        assertEquals(listOf(3), breakdown.transcriptDetail!!.volunteerDefaultedYears)
    }

    // ── 결측 학기 대체 ────────────────────────────────────────────

    @Test
    fun `결측 학기는 같은 학년 다른 학기 성적으로 대체한다`() {
        // 1-2 없음, 1-1 있음 → 1-1로 대체
        val semesters = mapOf(
            semester(1, 1) to listOf(A, B, C, D, E), // 15 ÷ 25 = 0.6 → × 18 = 10.8
            semester(2, 1) to fullMarks(),
            semester(2, 2) to fullMarks(),
            semester(3, 1) to fullMarks(),
        )
        val detail = engine.score(transcript(semesters = semesters)).transcriptDetail!!

        assertEquals(BigDecimal("10.800"), detail.semesterScores.getValue(semester(1, 2)))
        assertEquals(
            SemesterSource.Substituted(semester(1, 1), MissingSemesterStrategy.SAME_YEAR_OTHER_SEMESTER),
            detail.semesterSources.getValue(semester(1, 2)),
        )
    }

    @Test
    fun `학년 전체가 결측이면 차상위 학년 성적을 학기별로 적용한다`() {
        // 1학년 전체 없음 → 1-2는 차상위(2학년) 2학기 성적으로 대체
        val semesters = mapOf(
            semester(2, 1) to fullMarks(),
            semester(2, 2) to listOf(A, B, C, D, E), // 0.6
            semester(3, 1) to fullMarks(),
        )
        val detail = engine.score(transcript(semesters = semesters)).transcriptDetail!!

        // 1-2 = 0.6 × 18 = 10.8
        assertEquals(BigDecimal("10.800"), detail.semesterScores.getValue(semester(1, 2)))
        assertEquals(
            SemesterSource.Substituted(semester(2, 2), MissingSemesterStrategy.UPPER_YEAR),
            detail.semesterSources.getValue(semester(1, 2)),
        )
    }

    @Test
    fun `대체된 학기를 다시 참조하면 대체 결과를 이어받는다`() {
        // 2-2 없음 → 2-1로 대체. 1학년 전체 없음 → 1-2는 (대체된) 2-2를 이어받아 2-1 값이 된다
        val semesters = mapOf(
            semester(2, 1) to listOf(A, B, C, D, E), // 0.6
            semester(3, 1) to fullMarks(),
        )
        val detail = engine.score(transcript(semesters = semesters)).transcriptDetail!!

        assertEquals(BigDecimal("27.000"), detail.semesterScores.getValue(semester(2, 2))) // 0.6 × 45
        assertEquals(BigDecimal("10.800"), detail.semesterScores.getValue(semester(1, 2))) // 0.6 × 18
    }

    @Test
    fun `졸업자의 3학년 결측은 차하위 학년 성적을 학기별로 적용한다`() {
        // 3학년 전체 없음 → 차상위 학년이 없으므로 차하위(2학년) 학기별 적용
        val semesters = mapOf(
            semester(2, 1) to listOf(A, B, C, D, E), // 0.6
            semester(2, 2) to fullMarks(),
        )
        val detail = engine.score(
            transcript(type = GRADUATE, semesters = semesters),
        ).transcriptDetail!!

        assertEquals(BigDecimal("32.400"), detail.semesterScores.getValue(semester(3, 1))) // 0.6 × 54
        assertEquals(BigDecimal("54.000"), detail.semesterScores.getValue(semester(3, 2))) // 1.0 × 54
        assertEquals(
            SemesterSource.Substituted(semester(2, 1), MissingSemesterStrategy.LOWER_YEAR),
            detail.semesterSources.getValue(semester(3, 1)),
        )
    }

    @Test
    fun `어떤 규칙으로도 대체할 수 없는 결측 학기는 산출 불가 오류다`() {
        val semesters = mapOf(semester(3, 1) to fullMarks())

        assertFailsWith<ScoringException> {
            engine.score(transcript(semesters = semesters))
        }
    }

    // ── 검정고시 ─────────────────────────────────────────────────

    @Test
    fun `검정고시 평균 100점이면 총점 300이다`() {
        val breakdown = engine.score(StudentRecord.Ged(BigDecimal(100)))

        assertEquals(BigDecimal("240.000"), breakdown.subjectsScore)
        assertEquals(BigDecimal("30.000"), breakdown.attendanceScore)
        assertEquals(BigDecimal("30.000"), breakdown.volunteerScore)
        assertEquals(BigDecimal("300.000"), breakdown.totalScore)
        assertEquals(null, breakdown.transcriptDetail)
    }

    @Test
    fun `검정고시 교과는 (평균-50)÷50×240으로 환산한다`() {
        // (73.5 − 50) ÷ 50 × 240 = 112.8
        val breakdown = engine.score(StudentRecord.Ged(BigDecimal("73.5")))

        assertEquals(BigDecimal("112.800"), breakdown.subjectsScore)
        // 봉사 = (73.5 − 40) ÷ 60 × 30 = 16.75
        assertEquals(BigDecimal("16.750"), breakdown.volunteerScore)
        assertEquals(BigDecimal("46.750"), breakdown.nonSubjectsScore)
        assertEquals(BigDecimal("159.550"), breakdown.totalScore)
    }

    @Test
    fun `검정고시 환산식은 소수점 넷째 자리에서 반올림한다`() {
        // 교과 = 37.333 × 4.8 = 179.1984 → 179.198 / 봉사 = 47.333 ÷ 2 = 23.6665 → 23.667
        val breakdown = engine.score(StudentRecord.Ged(BigDecimal("87.333")))

        assertEquals(BigDecimal("179.198"), breakdown.subjectsScore)
        assertEquals(BigDecimal("23.667"), breakdown.volunteerScore)
        assertEquals(BigDecimal("232.865"), breakdown.totalScore)
    }

    @Test
    fun `검정고시 환산 결과가 음수면 0점 처리한다`() {
        // 평균 45: 교과 (45−50) → 음수 → 0 / 봉사 (45−40) ÷ 60 × 30 = 2.5
        val breakdown = engine.score(StudentRecord.Ged(BigDecimal(45)))

        assertEquals(BigDecimal("0.000"), breakdown.subjectsScore)
        assertEquals(BigDecimal("2.500"), breakdown.volunteerScore)
        assertEquals(BigDecimal("32.500"), breakdown.totalScore)
    }

    // ── 입력 검증 ─────────────────────────────────────────────────

    @Test
    fun `검정고시 유형으로 내신 레코드를 만들 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            transcript(type = GraduationType.GED)
        }
    }

    @Test
    fun `반영 학년 밖의 출결·봉사 자료는 산출 불가 오류다`() {
        val invalidAttendance = (1..4).associateWith { cleanAttendance() }
        assertFailsWith<ScoringException> {
            engine.score(transcript(attendance = invalidAttendance))
        }

        assertFailsWith<ScoringException> {
            engine.score(transcript(volunteer = mapOf(0 to 7, 1 to 7, 2 to 7, 3 to 7)))
        }
    }
}
