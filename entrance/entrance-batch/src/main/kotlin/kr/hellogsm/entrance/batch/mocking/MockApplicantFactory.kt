package kr.hellogsm.entrance.batch.mocking

import jakarta.persistence.EntityManager
import kr.hellogsm.entrance.batch.mapping.StudentRecordMapper
import kr.hellogsm.entrance.engine.scoring.ScoringEngine
import org.springframework.stereotype.Component
import team.themoment.hellogsmv3.domain.member.entity.Member
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType
import team.themoment.hellogsmv3.domain.member.entity.type.Sex
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestFactorsDetail
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult
import team.themoment.hellogsmv3.domain.oneseo.entity.MiddleSchoolAchievement
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

/**
 * go-hellogsm-ops 의 `generate-dml` 을 대체하는 mock 지원자 생성기.
 *
 * go 는 각 테이블의 점수 컬럼을 서로 무관하게 무작위로 채우지만(서류전형 총점도 두 난수의 합),
 * 여기서는 무작위 원본 성적(내신)만 만들고 [ScoringEngine] 에 그대로 태워 점수를 산출한다 —
 * 생성된 지원자가 실제 엔진(1차/2차 평가·배정)을 그대로 통과할 수 있는, 내적으로 일관된 데이터가 된다.
 */
@Component
class MockApplicantFactory(
    private val scoringEngine: ScoringEngine,
    private val entityManager: EntityManager,
) {

    /** 원서 배치 단계 — go-hellogsm-ops generate-dml 의 `-status` 값과 동일한 의미. */
    enum class Stage { FIRST, SECOND, FINAL_MAJOR, RE_EVALUATE }

    private val names = listOf("김하늘", "이도윤", "박서준", "최지우", "정민서", "강은우", "조수아", "윤지호")
    private val majors = Major.entries.toTypedArray()

    /** 지원자 한 명을 만들어 영속화하고 생성된 `oneseo_id` 를 반환한다. */
    fun createAndPersist(
        row: Int,
        screening: Screening,
        graduationType: GraduationType,
        stage: Stage,
        runTag: String,
        random: Random,
    ): Long {
        val member = Member.builder()
            .email("seed-$runTag-$row@test.hellogsm")
            .authReferrerType(AuthReferrerType.entries[random.nextInt(AuthReferrerType.entries.size)])
            .name(names[random.nextInt(names.size)])
            .sex(if (random.nextBoolean()) Sex.MALE else Sex.FEMALE)
            .phoneNumber("010%08d".format(random.nextInt(100_000_000)))
            .build()
        entityManager.persist(member)

        val (first, second, third) = majors.toList().shuffled(random).take(3)
        val submitPrefix = when (screening) {
            Screening.GENERAL -> "A"
            Screening.SPECIAL -> "B"
            Screening.EXTRA_ADMISSION, Screening.EXTRA_VETERANS -> "C"
        }

        val oneseo = Oneseo.builder()
            .member(member)
            .desiredMajors(
                DesiredMajors.builder()
                    .firstDesiredMajor(first)
                    .secondDesiredMajor(second)
                    .thirdDesiredMajor(third)
                    .build(),
            )
            .wantedScreening(screening)
            .appliedScreening(if (stage == Stage.FIRST) null else screening)
            .realOneseoArrivedYn(YesNo.YES)
            .passYn(
                when (stage) {
                    Stage.FINAL_MAJOR -> YesNo.YES
                    Stage.RE_EVALUATE -> YesNo.NO
                    Stage.FIRST, Stage.SECOND -> null
                },
            )
            .oneseoSubmitCode("$submitPrefix-$runTag-$row")
            .build()

        val achievement = randomAchievement(oneseo, graduationType, random)
        val record = StudentRecordMapper.toStudentRecord(achievement, graduationType)
        val breakdown = scoringEngine.score(record)

        val factorsDetail = EntranceTestFactorsDetail.builder()
            .generalSubjectsScore(breakdown.transcriptDetail?.generalSubjectsScore)
            .artsPhysicalSubjectsScore(breakdown.transcriptDetail?.artsSubjectsScore)
            .totalSubjectsScore(breakdown.subjectsScore)
            .attendanceScore(breakdown.attendanceScore)
            .volunteerScore(breakdown.volunteerScore)
            .totalNonSubjectsScore(breakdown.nonSubjectsScore)
            .build()

        val testResult = EntranceTestResult.builder()
            .oneseo(oneseo)
            .entranceTestFactorsDetail(factorsDetail)
            .documentEvaluationScore(breakdown.totalScore)
            .firstTestPassYn(if (stage == Stage.FIRST) null else YesNo.YES)
            .secondTestPassYn(
                when (stage) {
                    Stage.FIRST, Stage.SECOND -> null
                    Stage.FINAL_MAJOR -> YesNo.YES
                    Stage.RE_EVALUATE -> YesNo.NO
                },
            )
            .competencyEvaluationScore(if (stage == Stage.FIRST) null else randomScore(random))
            .interviewScore(if (stage == Stage.FIRST) null else randomScore(random))
            .build()

        val privacy = OneseoPrivacyDetail.builder()
            .oneseo(oneseo)
            .graduationType(graduationType)
            .graduationDate("2026.02")
            .address("광주광역시 광산구 상무대로 312")
            .detailAddress("${random.nextInt(1, 20)}동 ${random.nextInt(100, 2000)}호")
            .profileImg("https://placehold.co/seed-$runTag-$row.png")
            .guardianName("보호자$row")
            .guardianPhoneNumber("010%08d".format(random.nextInt(100_000_000)))
            .relationshipWithGuardian(if (random.nextBoolean()) "부" else "모")
            .schoolAddress(if (graduationType == GraduationType.GED) null else "광주광역시 광산구 상무대로 312")
            .schoolName(if (graduationType == GraduationType.GED) null else "광주소프트웨어마이스터중학교")
            .schoolTeacherName(if (graduationType == GraduationType.GED) null else "김선생")
            .schoolTeacherPhoneNumber(
                if (graduationType == GraduationType.GED) null else "010%08d".format(random.nextInt(100_000_000)),
            )
            .build()

        oneseo.modifyMiddleSchoolAchievement(achievement)
        oneseo.modifyOneseoPrivacyDetail(privacy)
        oneseo.modifyEntranceTestResult(testResult)

        entityManager.persist(oneseo)
        return oneseo.id
    }

    /**
     * 졸업 구분별 원본 성적 무작위 생성. 학기 구성은 plan 의 요건([Plan.kt]의
     * `transcript(CANDIDATE)`/`transcript(GRADUATE)`)과 정확히 맞춘다 — 결측 학기 대체 경로를
     * 타지 않도록, 해당 없는 학기는 애초에 채우지 않는다(예: 졸업자는 1-2 없음, 재학생은 3-2 없음).
     */
    private fun randomAchievement(oneseo: Oneseo, graduationType: GraduationType, random: Random): MiddleSchoolAchievement {
        val builder = MiddleSchoolAchievement.builder().oneseo(oneseo)

        return when (graduationType) {
            GraduationType.GED ->
                builder
                    .gedAvgScore(BigDecimal.valueOf(random.nextDouble(60.0, 100.0)).setScale(2, RoundingMode.HALF_UP))
                    .build()

            GraduationType.CANDIDATE ->
                builder
                    .achievement1_2(randomScores(random, 9, 1, 5))
                    .achievement2_1(randomScores(random, 9, 1, 5))
                    .achievement2_2(randomScores(random, 9, 1, 5))
                    .achievement3_1(randomScores(random, 9, 1, 5))
                    .achievement3_2(emptyList())
                    .artsPhysicalAchievement(randomScores(random, 9, 3, 5))
                    .absentDays(randomScores(random, 3, 0, 3))
                    .attendanceDays(randomScores(random, 9, 1, 5))
                    .volunteerTime(randomScores(random, 3, 0, 5))
                    .liberalSystem("자유학년제")
                    .generalSubjects(listOf("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어"))
                    .newSubjects(listOf("프로그래밍"))
                    .artsPhysicalSubjects(listOf("체육", "미술", "음악"))
                    .build()

            GraduationType.GRADUATE ->
                builder
                    .achievement2_1(randomScores(random, 9, 1, 5))
                    .achievement2_2(randomScores(random, 9, 1, 5))
                    .achievement3_1(randomScores(random, 9, 1, 5))
                    .achievement3_2(randomScores(random, 9, 1, 5))
                    .artsPhysicalAchievement(randomScores(random, 12, 3, 5))
                    .absentDays(randomScores(random, 3, 0, 3))
                    .attendanceDays(randomScores(random, 9, 1, 5))
                    .volunteerTime(randomScores(random, 3, 0, 5))
                    .generalSubjects(listOf("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어"))
                    .newSubjects(listOf("프로그래밍"))
                    .artsPhysicalSubjects(listOf("체육", "미술", "음악"))
                    .build()
        }
    }

    private fun randomScores(random: Random, length: Int, min: Int, max: Int): List<Int> =
        List(length) { random.nextInt(min, max + 1) }

    private fun randomScore(random: Random): BigDecimal =
        BigDecimal.valueOf(random.nextDouble(0.0, 100.0)).setScale(3, RoundingMode.HALF_UP)
}
