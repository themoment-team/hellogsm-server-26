package kr.hellogsm.entrance.engine.evaluation

import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.SemesterRef

/**
 * tools/golden/generate_batch_golden.py 가 생성한 golden fixture
 * (src/test/resources/golden/batch_parity.txt) 파서.
 *
 * 형식(파이프 구분, "-" = 값 없음):
 * - `S|시나리오명`
 * - `A|id|지망전형|GED여부|총점|비교과|일반교과|3-1|2-2|2-1|1-2|역량|면접|지망학과(;구분)`
 * - `F|id|1차 적용 전형` / `T|id|2차 적용 전형` / `C|id|2차 합성점수`
 * - `B|id` (2차 미응시) / `M|id|배정 학과`
 */
internal data class GoldenApplicant(
    val applicant: RoundApplicant,
    val choices: List<String>,
)

internal data class BatchScenario(
    val name: String,
    val applicants: List<GoldenApplicant>,
    /** 1차 결과: id → 적용 전형 (null = 탈락) */
    val expectedFirstApplied: Map<String, String?>,
    /** 2차 결과: 2차 참가자 id → 적용 전형 (null = 탈락, 미응시자는 [expectedAbsent]) */
    val expectedSecondApplied: Map<String, String?>,
    /** 2차 합성 점수: 2차 참가자(미응시 제외) id → 점수 */
    val expectedSecondScore: Map<String, String>,
    val expectedAbsent: List<String>,
    /** 최종 합격자 id → 배정 학과 */
    val expectedMajors: Map<String, String>,
)

internal fun loadBatchParityScenarios(): List<BatchScenario> {
    val resource = requireNotNull(
        BatchScenario::class.java.getResourceAsStream("/golden/batch_parity.txt"),
    ) { "golden fixture 리소스가 없음 — tools/golden/generate_batch_golden.py 실행 필요" }

    val scenarios = mutableListOf<BatchScenario>()
    var builder: ScenarioBuilder? = null

    resource.bufferedReader().forEachLine { line ->
        if (line.isBlank() || line.startsWith("#")) return@forEachLine
        val fields = line.split("|")
        when (fields[0]) {
            "S" -> {
                builder?.let { scenarios += it.build() }
                builder = ScenarioBuilder(fields[1])
            }

            else -> requireNotNull(builder) { "시나리오 선언(S|...) 전에 데이터가 나옴: $line" }.accept(fields)
        }
    }
    builder?.let { scenarios += it.build() }
    return scenarios
}

private class ScenarioBuilder(private val name: String) {
    private val applicants = mutableListOf<GoldenApplicant>()
    private val first = linkedMapOf<String, String?>()
    private val second = linkedMapOf<String, String?>()
    private val secondScore = linkedMapOf<String, String>()
    private val absent = mutableListOf<String>()
    private val majors = linkedMapOf<String, String>()

    fun accept(fields: List<String>) {
        fun opt(value: String): String? = value.takeUnless { it == "-" }
        when (fields[0]) {
            "A" -> applicants += parseApplicant(fields)
            "F" -> first[fields[1]] = opt(fields[2])
            "T" -> second[fields[1]] = opt(fields[2])
            "C" -> secondScore[fields[1]] = fields[2]
            "B" -> absent += fields[1]
            "M" -> majors[fields[1]] = fields[2]
            else -> error("알 수 없는 레코드: $fields")
        }
    }

    private fun parseApplicant(f: List<String>): GoldenApplicant {
        val (id, wanted, ged) = Triple(f[1], f[2], f[3] == "1")
        val manualScores = buildMap {
            f[11].takeUnless { it == "-" }?.let { put("COMPETENCY", it) }
            f[12].takeUnless { it == "-" }?.let { put("INTERVIEW", it) }
        }
        val semesterSlots = listOf(SemesterRef(3, 1), SemesterRef(2, 2), SemesterRef(2, 1), SemesterRef(1, 2))
        val semesters =
            if (ged) emptyMap()
            else semesterSlots.withIndex().associate { (i, slot) -> slot to f[7 + i] }

        return GoldenApplicant(
            applicant = testApplicant(
                id = id,
                screening = wanted,
                total = f[4],
                nonSubjects = f[5],
                general = f[6].takeUnless { it == "-" },
                semesters = semesters,
                graduationType = if (ged) GraduationType.GED else GraduationType.CANDIDATE,
                manualScores = manualScores,
            ),
            choices = f[13].split(";"),
        )
    }

    fun build() = BatchScenario(
        name = name,
        applicants = applicants,
        expectedFirstApplied = first,
        expectedSecondApplied = second,
        expectedSecondScore = secondScore,
        expectedAbsent = absent,
        expectedMajors = majors,
    )
}
