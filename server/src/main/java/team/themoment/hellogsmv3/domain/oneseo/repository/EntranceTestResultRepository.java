package team.themoment.hellogsmv3.domain.oneseo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

public interface EntranceTestResultRepository extends JpaRepository<EntranceTestResult, Long> {
    EntranceTestResult findByOneseo(Oneseo oneseo);

    boolean existsByFirstTestPassYnIsNotNull();

    boolean existsByFirstTestPassYnIsNull();

    /**
     * 1차 합격 여부가 주어졌을 때, 2차 합격 여부가 아직 결정되지 않은 지원자가 존재하는지 확인합니다.
     *
     * @param firstTestPassYn
     *            1차 합격 여부(YES or NO)
     * @return 1차 합격 여부가 주어졌을 때, 2차 합격 여부가 아직 결정되지 않은 지원자가 존재하면 true, 그렇지 않으면 false
     * @author 김태은
     */
    boolean existsByFirstTestPassYnAndSecondTestPassYnIsNull(YesNo firstTestPassYn);
}
