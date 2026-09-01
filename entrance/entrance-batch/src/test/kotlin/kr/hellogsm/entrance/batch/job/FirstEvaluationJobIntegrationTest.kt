package kr.hellogsm.entrance.batch.job

import jakarta.persistence.EntityManager
import kr.hellogsm.entrance.batch.persistence.BatchOneseoRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import team.themoment.hellogsmv3.domain.member.entity.Member
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType
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
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * entrance-batch 전 구간 통합 테스트 — 실제 MySQL(Testcontainers)에 지원자를 시드하고
 * `first-eval` 잡을 돌려 DB write-back(엔티티↔엔진 매핑·mutator·repository)이 동작하는지 검증.
 *
 * 시드 성적은 scoring golden CANDIDATE-001(총점 156.212)과 동일하며, 저장
 * documentEvaluationScore 를 그 값으로 두어 대조 검증이 불일치 없이 통과하는지도 확인한다.
 */
@SpringBootTest
@Testcontainers
class FirstEvaluationJobIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.0")

        @DynamicPropertySource
        @JvmStatic
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            // 컨테이너가 클래스 종료 직후 버려지는 일회용이라 DROP이 불필요함 — create-drop을 쓰면
            // 컨텍스트 종료 시(JVM 셧다운 훅) 이미 죽은 컨테이너에 DROP DDL을 시도하며 HikariCP
            // 커넥션 타임아웃(기본 30초)만큼 그대로 멈춘다.
            registry.add("spring.jpa.hibernate.ddl-auto") { "create" }
        }
    }

    @Autowired lateinit var firstEvaluationJob: FirstEvaluationJob
    @Autowired lateinit var oneseoRepository: BatchOneseoRepository
    @Autowired lateinit var entityManager: EntityManager
    @Autowired lateinit var txManager: PlatformTransactionManager

    @Test
    fun `first-eval 잡이 1차 합격 여부와 적용 전형을 DB에 기록한다`() {
        val oneseoId = seedCandidate001()

        firstEvaluationJob.run(dryRun = false)

        val tx = TransactionTemplate(txManager)
        tx.execute {
            val oneseo = oneseoRepository.findById(oneseoId).orElseThrow()
            assertEquals(YesNo.YES, oneseo.entranceTestResult.firstTestPassYn, "1차 합격 여부")
            assertEquals(Screening.GENERAL, oneseo.appliedScreening, "적용 전형(일반전형)")
        }
    }

    /** scoring golden CANDIDATE-001 원본으로 지원자 한 명을 시드하고 oneseoId 반환. */
    private fun seedCandidate001(): Long {
        val tx = TransactionTemplate(txManager)
        return tx.execute {
            val member = Member.builder()
                .email("cand001@test.hellogsm")
                .authReferrerType(AuthReferrerType.entries.first())
                .build()
            entityManager.persist(member)

            val oneseo = Oneseo.builder()
                .member(member)
                .desiredMajors(
                    DesiredMajors.builder()
                        .firstDesiredMajor(Major.AI)
                        .secondDesiredMajor(Major.SW)
                        .thirdDesiredMajor(Major.IOT)
                        .build(),
                )
                .wantedScreening(Screening.GENERAL)
                .realOneseoArrivedYn(YesNo.YES)
                .build()

            val achievement = MiddleSchoolAchievement.builder()
                .oneseo(oneseo)
                .achievement1_2(listOf(3, 3, 3, 4, 5, 3, 1, 4, 2, 1))
                .achievement2_1(listOf(1, 3, 3, 1, 3, 5, 3))
                .achievement2_2(listOf(3, 4, 3, 1, 1, 5, 1))
                .achievement3_1(listOf(4, 5, 2, 4, 1, 3, 3, 5, 3, 1, 2))
                .achievement3_2(emptyList())
                .artsPhysicalAchievement(listOf(3, 3, 4, 5))
                .absentDays(listOf(4, 1, 2))
                .attendanceDays(listOf(0, 3, 2, 4, 1, 2, 4, 0, 1))
                .volunteerTime(listOf(5, 2, 2))
                .build()

            val privacy = OneseoPrivacyDetail.builder()
                .oneseo(oneseo)
                .graduationType(GraduationType.CANDIDATE)
                .graduationDate("2026.02")
                .address("광주광역시")
                .detailAddress("상세주소")
                .profileImg("profile.png")
                .guardianName("보호자")
                .guardianPhoneNumber("01000000000")
                .relationshipWithGuardian("부")
                .build()

            val testResult = EntranceTestResult.builder()
                .oneseo(oneseo)
                .entranceTestFactorsDetail(EntranceTestFactorsDetail.builder().build())
                .documentEvaluationScore(BigDecimal("156.212"))
                .build()

            oneseo.modifyMiddleSchoolAchievement(achievement)
            oneseo.modifyOneseoPrivacyDetail(privacy)
            oneseo.modifyEntranceTestResult(testResult)

            entityManager.persist(oneseo)
            entityManager.flush()
            oneseo.id
        }!!
    }
}
