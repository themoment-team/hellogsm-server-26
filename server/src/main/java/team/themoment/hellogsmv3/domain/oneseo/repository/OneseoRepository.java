package team.themoment.hellogsmv3.domain.oneseo.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.custom.CustomOneseoRepository;

public interface OneseoRepository extends JpaRepository<Oneseo, Long>, CustomOneseoRepository {
    boolean existsByMember(Member member);

    Optional<Oneseo> findByMember(Member member);

    @Query("SELECT o.member.id FROM Oneseo o WHERE o.oneseoSubmitCode = :submitCode")
    Optional<Long> findMemberIdByOneseoSubmitCode(@Param("submitCode") String submitCode);

    void deleteByOneseoSubmitCode(String oneseoSubmitCode);
}
