package kr.hellogsm.entrance.batch.persistence

import org.springframework.data.jpa.repository.JpaRepository
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo

/**
 * 배치 전용 최소 repository. 서버의 custom(QueryDSL·DTO 투영) repository 와 달리
 * 배치는 엔티티를 통째로 읽어 엔진에 넘기고 결과 필드만 갱신하므로 단순 조회로 충분하다.
 * 엔티티(스키마 매핑)는 persistence 모듈과 공유하고, 쿼리 인터페이스만 배치가 따로 둔다.
 */
interface BatchOneseoRepository : JpaRepository<Oneseo, Long>

interface BatchEntranceTestResultRepository : JpaRepository<EntranceTestResult, Long>
