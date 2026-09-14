package team.themoment.hellogsmv3.domain.oneseo.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@Table(name = "tb_entrance_test_factors_detail")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EntranceTestFactorsDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entrance_test_factors_detail_id")
    private Long id;

    @Column(name = "general_subjects_score", precision = 6, scale = 3)
    private BigDecimal generalSubjectsScore;

    @Column(name = "arts_physical_subjects_score", precision = 6, scale = 3)
    private BigDecimal artsPhysicalSubjectsScore;

    @Column(name = "total_subjects_score", precision = 6, scale = 3)
    private BigDecimal totalSubjectsScore;

    @Column(name = "attendance_score", precision = 6, scale = 3)
    private BigDecimal attendanceScore;

    @Column(name = "volunteer_score", precision = 6, scale = 3)
    private BigDecimal volunteerScore;

    @Column(name = "total_non_subjects_score", precision = 6, scale = 3)
    private BigDecimal totalNonSubjectsScore;

    @Column(name = "score_1_2", precision = 6, scale = 3)
    private BigDecimal score1_2;

    @Column(name = "score_2_1", precision = 6, scale = 3)
    private BigDecimal score2_1;

    @Column(name = "score_2_2", precision = 6, scale = 3)
    private BigDecimal score2_2;

    @Column(name = "score_3_1", precision = 6, scale = 3)
    private BigDecimal score3_1;

    @Column(name = "score_3_2", precision = 6, scale = 3)
    private BigDecimal score3_2;

    public void updateGradeEntranceTestFactorsDetail(BigDecimal generalSubjectsScore,
            BigDecimal artsPhysicalSubjectsScore,
            BigDecimal totalSubjectsScore,
            BigDecimal attendanceScore,
            BigDecimal volunteerScore,
            BigDecimal totalNonSubjectsScore,
            BigDecimal score1_2,
            BigDecimal score2_1,
            BigDecimal score2_2,
            BigDecimal score3_1,
            BigDecimal score3_2) {
        this.generalSubjectsScore = generalSubjectsScore;
        this.artsPhysicalSubjectsScore = artsPhysicalSubjectsScore;
        this.totalSubjectsScore = totalSubjectsScore;
        this.attendanceScore = attendanceScore;
        this.volunteerScore = volunteerScore;
        this.totalNonSubjectsScore = totalNonSubjectsScore;
        this.score1_2 = score1_2;
        this.score2_1 = score2_1;
        this.score2_2 = score2_2;
        this.score3_1 = score3_1;
        this.score3_2 = score3_2;
    }

    public void updateGedEntranceTestFactorsDetail(BigDecimal attendanceScore,
            BigDecimal volunteerScore,
            BigDecimal totalSubjectsScore,
            BigDecimal totalNonSubjectsScore) {
        this.attendanceScore = attendanceScore;
        this.volunteerScore = volunteerScore;
        this.totalSubjectsScore = totalSubjectsScore;
        this.totalNonSubjectsScore = totalNonSubjectsScore;
        this.generalSubjectsScore = null;
        this.artsPhysicalSubjectsScore = null;
        this.score1_2 = null;
        this.score2_1 = null;
        this.score2_2 = null;
        this.score3_1 = null;
        this.score3_2 = null;
    }
}
