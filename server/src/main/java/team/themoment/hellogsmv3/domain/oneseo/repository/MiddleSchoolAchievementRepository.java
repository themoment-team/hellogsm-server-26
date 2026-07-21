package team.themoment.hellogsmv3.domain.oneseo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import team.themoment.hellogsmv3.domain.oneseo.entity.MiddleSchoolAchievement;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;

public interface MiddleSchoolAchievementRepository extends JpaRepository<MiddleSchoolAchievement, Long> {
    MiddleSchoolAchievement findByOneseo(Oneseo oneseo);
}
