package kr.hellogsm.entrance.batch.job

import kr.hellogsm.entrance.batch.persistence.BatchOneseoRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * CLI 진입점. ops 가 `--job=<이름> [--dry-run]` 으로 실행한다.
 * 사용 가능한 잡: status, first-eval, second-eval, assign.
 */
@Component
class JobDispatcher(
    private val jobs: List<BatchJob>,
    private val oneseoRepository: BatchOneseoRepository,
) : CommandLineRunner {

    override fun run(vararg args: String) {
        val options = args
            .filter { it.startsWith("--") }
            .associate {
                val parts = it.removePrefix("--").split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "true")
            }

        val jobName = options["job"] ?: "status"
        val dryRun = options["dry-run"].toBoolean()

        when (jobName) {
            "status" -> println("[status] 접수 대상 원서 수: ${oneseoRepository.count()}")
            else -> {
                val job = jobs.firstOrNull { it.name == jobName }
                    ?: error("알 수 없는 job: '$jobName'. 사용 가능: status, ${jobs.joinToString(", ") { it.name }}")
                job.run(dryRun)
            }
        }
    }
}
