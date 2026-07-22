package kr.hellogsm.entrance.batch.job

import kr.hellogsm.entrance.batch.persistence.BatchOneseoRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

/**
 * CLI 진입점. ops 가 `--job=<이름> [--dry-run]` 으로 실행한다.
 *
 * 현재는 배선 검증용 `status` 잡만 구현돼 있고, 실제 파이프라인(first-eval·second-eval·
 * assign)은 다음 증분에서 추가한다 — 성적 재계산 매핑(MiddleSchoolAchievement→StudentRecord,
 * 특히 출결)은 go-hellogsm 대비 parity 확인이 필요하다.
 */
@Component
class JobDispatcher(
    private val oneseoRepository: BatchOneseoRepository,
) : CommandLineRunner {

    override fun run(vararg args: String) {
        val options = args
            .filter { it.startsWith("--") }
            .associate {
                val parts = it.removePrefix("--").split("=", limit = 2)
                parts[0] to (parts.getOrNull(1) ?: "true")
            }

        val job = options["job"] ?: "status"
        val dryRun = options["dry-run"].toBoolean()

        when (job) {
            "status" -> printStatus()
            else -> error(
                "알 수 없는 job: '$job'. 사용 가능: status" +
                    " (first-eval·second-eval·assign 은 다음 증분에서 구현). dryRun=$dryRun",
            )
        }
    }

    private fun printStatus() {
        println("[entrance-batch] DB 연결 확인 — 총 원서 수: ${oneseoRepository.count()}")
    }
}
