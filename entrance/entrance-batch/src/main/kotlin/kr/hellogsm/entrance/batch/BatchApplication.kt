package kr.hellogsm.entrance.batch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/**
 * entrance-batch — go-hellogsm 배치를 대체하는 CLI 러너.
 *
 * 엔진(순수)·요강(entrance-plans)·공유 영속성(persistence)을 잇는 어댑터다.
 * JPA 엔티티는 persistence 모듈(team.themoment.hellogsmv3.*)에 있으므로 명시적으로
 * [EntityScan] 하고, repository 는 배치 자체 패키지에서 스캔한다. auditing 은 각 앱이
 * 켜야 하므로 여기서 활성화한다([EnableJpaAuditing]).
 */
@SpringBootApplication
@EnableJpaAuditing
@EntityScan("team.themoment.hellogsmv3")
@EnableJpaRepositories("kr.hellogsm.entrance.batch")
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}
