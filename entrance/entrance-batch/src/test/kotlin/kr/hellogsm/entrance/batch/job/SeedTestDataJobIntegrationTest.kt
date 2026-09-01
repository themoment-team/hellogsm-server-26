package kr.hellogsm.entrance.batch.job

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `seed-testdata` 잡이 실제 MySQL(Testcontainers)에 mock 지원자를 생성하는지 검증한다.
 */
@SpringBootTest
@Testcontainers
class SeedTestDataJobIntegrationTest {

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

    @Autowired lateinit var seedTestDataJob: SeedTestDataJob
    @Autowired lateinit var oneseoRepository: kr.hellogsm.entrance.batch.persistence.BatchOneseoRepository

    @Test
    fun `dry-run 은 DB에 아무것도 쓰지 않는다`() {
        seedTestDataJob.run(dryRun = true, options = mapOf("screening" to "GEN3,SPE2,EXT1", "status" to "FIRST"))

        assertEquals(0, oneseoRepository.count(), "dry-run 은 저장하지 않아야 함")
    }

    @Test
    fun `screening 파라미터대로 지원자를 생성해 저장한다`() {
        seedTestDataJob.run(
            dryRun = false,
            options = mapOf("screening" to "GEN3,SPE2,EXT1", "status" to "SECOND", "graduate" to "CANDIDATE"),
        )

        val saved = oneseoRepository.findAll()
        assertEquals(6, saved.size, "GEN3+SPE2+EXT1 = 6명 저장되어야 함")
        assertEquals(3, saved.count { it.wantedScreening.name == "GENERAL" })
        assertEquals(2, saved.count { it.wantedScreening.name == "SPECIAL" })
        assertEquals(1, saved.count { it.wantedScreening.name == "EXTRA_ADMISSION" || it.wantedScreening.name == "EXTRA_VETERANS" })
        saved.forEach {
            assertEquals("YES", it.entranceTestResult.firstTestPassYn?.name, "SECOND 단계는 1차 합격 처리되어야 함")
            assertEquals(null, it.entranceTestResult.secondTestPassYn, "SECOND 단계는 2차 결과 미정이어야 함")
        }
    }
}
