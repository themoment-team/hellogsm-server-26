package kr.hellogsm.entrance.batch.job

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `seed-testdata` 잡이 실제 MySQL(Testcontainers)에 mock 지원자를 생성하는지,
 * 그리고 confirm 프롬프트에서 'yes' 가 아니면 아무것도 쓰지 않는지 검증한다.
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
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }

    @Autowired lateinit var seedTestDataJob: SeedTestDataJob
    @Autowired lateinit var oneseoRepository: kr.hellogsm.entrance.batch.persistence.BatchOneseoRepository

    private val originalIn = System.`in`

    private fun withStdin(input: String, block: () -> Unit) {
        System.setIn(ByteArrayInputStream(input.toByteArray()))
        try {
            block()
        } finally {
            System.setIn(originalIn)
        }
    }

    @Test
    fun `dry-run 은 DB에 아무것도 쓰지 않는다`() {
        seedTestDataJob.run(dryRun = true, options = mapOf("screening" to "GEN3,SPE2,EXT1", "status" to "FIRST"))

        assertEquals(0, oneseoRepository.count(), "dry-run 은 저장하지 않아야 함")
    }

    @Test
    fun `yes 를 입력하지 않으면 취소되어 아무것도 쓰지 않는다`() {
        withStdin("no\n") {
            seedTestDataJob.run(dryRun = false, options = mapOf("screening" to "GEN2", "status" to "FIRST"))
        }

        assertEquals(0, oneseoRepository.count(), "confirm 을 거부하면 저장하지 않아야 함")
    }

    @Test
    fun `yes 를 입력하면 screening 파라미터대로 지원자를 생성해 저장한다`() {
        withStdin("yes\n") {
            seedTestDataJob.run(
                dryRun = false,
                options = mapOf("screening" to "GEN3,SPE2,EXT1", "status" to "SECOND", "graduate" to "CANDIDATE"),
            )
        }

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
