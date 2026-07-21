package team.themoment.hellogsmv3.domain.common.operation.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import team.themoment.hellogsmv3.domain.common.operation.entity.OperationTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

public interface OperationTestResultRepository extends JpaRepository<OperationTestResult, Long> {
    @Query("SELECT o FROM OperationTestResult o WHERE o.id = 1")
    Optional<OperationTestResult> findTestResult();

    boolean existsByFirstTestResultAnnouncementYn(YesNo firstTestResultAnnouncementYn);
}
