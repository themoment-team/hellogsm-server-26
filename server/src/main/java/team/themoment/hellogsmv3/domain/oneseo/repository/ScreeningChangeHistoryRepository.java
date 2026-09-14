package team.themoment.hellogsmv3.domain.oneseo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import team.themoment.hellogsmv3.domain.oneseo.entity.WantedScreeningChangeHistory;

public interface ScreeningChangeHistoryRepository extends JpaRepository<WantedScreeningChangeHistory, Long> {
}
