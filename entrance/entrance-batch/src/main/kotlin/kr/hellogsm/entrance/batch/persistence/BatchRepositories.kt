package kr.hellogsm.entrance.batch.persistence

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo

/**
 * 배치 전용 최소 repository. 서버의 custom(QueryDSL·DTO 투영) repository 와 달리
 * 배치는 엔티티를 통째로 읽어 엔진에 넘기고 결과 필드만 갱신하므로 단순 조회로 충분하다.
 * 엔티티(스키마 매핑)는 persistence 모듈과 공유하고, 쿼리 인터페이스만 배치가 따로 둔다.
 */
interface BatchOneseoRepository : JpaRepository<Oneseo, Long> {
    /**
     * 실제 원서가 도착한(접수 완료) 지원자 — 전형 대상.
     *
     * `Oneseo` 의 `@OneToOne(mappedBy = ...)` 연관 3개는 프록시를 만들 수 없어 기본이 즉시 로딩인데,
     * 그대로 두면 지원자 1명당 SELECT 3번이 추가로 나간다(N+1). 배치는 전체 지원자를 한 번에 읽고
     * 세 연관을 모두 쓰므로 `@EntityGraph` 로 한 쿼리에 함께 가져온다.
     */
    @EntityGraph(attributePaths = ["entranceTestResult", "oneseoPrivacyDetail", "middleSchoolAchievement"])
    fun findAllByRealOneseoArrivedYn(realOneseoArrivedYn: YesNo): List<Oneseo>
}

interface BatchEntranceTestResultRepository : JpaRepository<EntranceTestResult, Long>
