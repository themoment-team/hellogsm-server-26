package kr.hellogsm.entrance.batch.job

import kr.hellogsm.entrance.batch.mocking.MockApplicantFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening
import kotlin.random.Random

/**
 * 로컬 개발·통합테스트용 mock 지원자를 생성해 DB 에 저장하는 잡. go-hellogsm-ops 의
 * `generate-dml`(SQL 파일 생성)과 달리 이 잡은 엔티티를 만들어 곧바로 insert 한다.
 *
 * 사용례:
 * ```
 * --job=seed-testdata --screening=GEN10,SPE5,EXT2 --status=FIRST --graduate=RANDOM
 * ```
 * - `--screening` (필수): `GEN`/`SPE`/`EXT` 뒤에 인원수, `,` 로 구분 (EXT 는 국가보훈/특례 중 무작위 배정)
 * - `--status` (필수): `FIRST`/`SECOND`/`FINAL_MAJOR`/`RE_EVALUATE` — 어느 배치 단계 직전 상태로 만들지
 * - `--graduate` (선택, 기본 RANDOM): `CANDIDATE`/`GRADUATE`/`GED`/`RANDOM`
 *
 * `--dry-run` 이면 생성 개수만 보고하고 DB 에는 쓰지 않는다.
 */
@Component
class SeedTestDataJob(
    private val factory: MockApplicantFactory,
    private val txManager: PlatformTransactionManager,
) : BatchJob {

    override val name = "seed-testdata"

    override fun run(dryRun: Boolean, options: Map<String, String>) {
        val screeningCounts = parseScreening(
            options["screening"] ?: error("--screening 은 필수입니다 (예: GEN10,SPE5,EXT2)"),
        )
        val stage = parseStage(options["status"] ?: error("--status 는 필수입니다 (FIRST/SECOND/FINAL_MAJOR/RE_EVALUATE)"))
        val graduateParam = (options["graduate"] ?: "RANDOM").uppercase()

        val random = Random.Default
        val rows = screeningCounts.values.sum()
        val screenings = resolveScreenings(screeningCounts, random)
        val graduationTypes = resolveGraduationTypes(graduateParam, rows, random)

        println(
            "[seed-testdata] 생성 대상 ${rows}명 · " +
                "GEN ${screeningCounts.getValue(ScreeningParam.GEN)}/SPE ${screeningCounts.getValue(ScreeningParam.SPE)}/" +
                "EXT ${screeningCounts.getValue(ScreeningParam.EXT)} · status=$stage · graduate=$graduateParam",
        )

        if (dryRun) {
            println("[seed-testdata] dry-run — DB에 쓰지 않음")
            return
        }

        val runTag = random.nextInt(100_000, 999_999).toString()
        val tx = TransactionTemplate(txManager)
        val insertedIds = tx.execute {
            (1..rows).map { row ->
                factory.createAndPersist(row, screenings[row - 1], graduationTypes[row - 1], stage, runTag, random)
            }
        }
        println("[seed-testdata] 저장 완료 · ${insertedIds.size}명 (oneseo_id ${insertedIds.minOrNull()}..${insertedIds.maxOrNull()})")
    }

    private enum class ScreeningParam { GEN, SPE, EXT }

    private fun parseScreening(raw: String): Map<ScreeningParam, Int> {
        val counts = raw.split(",").associate { token ->
            val prefix = token.take(3).uppercase()
            val count = token.drop(3).toIntOrNull()
                ?: error("전형 지원자 수를 정수로 변환할 수 없습니다: '$token' (예: GEN10,SPE5,EXT2)")
            val param = ScreeningParam.entries.find { it.name == prefix }
                ?: error("알 수 없는 전형 코드: '$prefix' (GEN/SPE/EXT 중 하나)")
            param to count
        }
        return ScreeningParam.entries.associateWith { counts[it] ?: 0 }
    }

    private fun parseStage(raw: String): MockApplicantFactory.Stage =
        MockApplicantFactory.Stage.entries.find { it.name == raw.uppercase() }
            ?: error("알 수 없는 원서상태: '$raw' (FIRST/SECOND/FINAL_MAJOR/RE_EVALUATE 중 하나)")

    /** go-hellogsm-ops 와 동일한 순서: GEN 블록 → SPE 블록 → EXT 블록(국가보훈/특례 무작위). */
    private fun resolveScreenings(counts: Map<ScreeningParam, Int>, random: Random): List<Screening> {
        val list = ArrayList<Screening>(counts.values.sum())
        repeat(counts.getValue(ScreeningParam.GEN)) { list += Screening.GENERAL }
        repeat(counts.getValue(ScreeningParam.SPE)) { list += Screening.SPECIAL }
        repeat(counts.getValue(ScreeningParam.EXT)) {
            list += if (random.nextBoolean()) Screening.EXTRA_ADMISSION else Screening.EXTRA_VETERANS
        }
        return list
    }

    private fun resolveGraduationTypes(graduateParam: String, rows: Int, random: Random): List<GraduationType> {
        if (graduateParam == "RANDOM") {
            return List(rows) { GraduationType.entries[random.nextInt(GraduationType.entries.size)] }
        }
        val fixed = GraduationType.entries.find { it.name == graduateParam }
            ?: error("알 수 없는 졸업상태: '$graduateParam' (CANDIDATE/GRADUATE/GED/RANDOM 중 하나)")
        return List(rows) { fixed }
    }
}
