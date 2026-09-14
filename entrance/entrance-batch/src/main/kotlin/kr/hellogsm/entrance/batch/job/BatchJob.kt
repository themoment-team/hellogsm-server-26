package kr.hellogsm.entrance.batch.job

/**
 * ops 가 CLI 로 실행하는 배치 잡. `--dry-run` 이면 계산·리포트만 하고 DB write 는 생략한다.
 * 각 잡은 상류 단계를 재계산하는 멱등 실행이다(재실행하면 결과를 덮어쓴다).
 *
 * [options] 는 `--job`/`--dry-run` 을 제외한 나머지 `--key=value` 인자 전체다
 * (예: `seed-testdata` 의 `--screening`/`--status`/`--graduate`). 대부분의 잡은 사용하지 않는다.
 */
interface BatchJob {
    val name: String

    fun run(dryRun: Boolean, options: Map<String, String> = emptyMap())
}
