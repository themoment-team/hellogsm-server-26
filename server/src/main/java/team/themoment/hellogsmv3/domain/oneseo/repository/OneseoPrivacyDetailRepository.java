package team.themoment.hellogsmv3.domain.oneseo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail;

public interface OneseoPrivacyDetailRepository extends JpaRepository<OneseoPrivacyDetail, Long> {
    OneseoPrivacyDetail findByOneseo(Oneseo oneseo);
}
