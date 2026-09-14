package team.themoment.hellogsmv3.domain.member.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByAuthReferrerTypeAndEmail(AuthReferrerType authRefType, String email);

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    @Query("DELETE FROM Member m WHERE m = :member")
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    void deleteDuplicate(Member member);

    @Lock(value = LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :memberId")
    Optional<Member> findByIdForUpdate(Long memberId);
}
