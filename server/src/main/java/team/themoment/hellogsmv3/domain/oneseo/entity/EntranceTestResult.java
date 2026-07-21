package team.themoment.hellogsmv3.domain.oneseo.entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Getter
@Entity
@Builder
@AllArgsConstructor
@Table(name = "tb_entrance_test_result", indexes = {
        @Index(name = "idx_first_test_pass_yn", columnList = "first_test_pass_yn"),
        @Index(name = "idx_second_test_pass_yn", columnList = "second_test_pass_yn")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntranceTestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entrance_test_result_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "oneseo_id")
    private Oneseo oneseo;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "entrance_test_factors_detail_id")
    private EntranceTestFactorsDetail entranceTestFactorsDetail;

    @Column(name = "document_evaluation_score", precision = 6, scale = 3)
    private BigDecimal documentEvaluationScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "first_test_pass_yn")
    private YesNo firstTestPassYn;

    @Column(name = "competency_evaluation_score")
    private BigDecimal competencyEvaluationScore;

    @Column(name = "interview_score")
    private BigDecimal interviewScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "second_test_pass_yn")
    private YesNo secondTestPassYn;

    public void modifyCompetencyEvaluationScore(BigDecimal competencyEvaluationScore) {
        this.competencyEvaluationScore = competencyEvaluationScore;
    }

    public void modifyInterviewScore(BigDecimal interviewScore) {
        this.interviewScore = interviewScore;
    }

    public void modifyDocumentEvaluationScore(BigDecimal documentEvaluationScore) {
        this.documentEvaluationScore = documentEvaluationScore;
    }

    public EntranceTestResult(Oneseo oneseo,
            EntranceTestFactorsDetail entranceTestFactorsDetail,
            BigDecimal documentEvaluationScore) {
        this.oneseo = oneseo;
        this.entranceTestFactorsDetail = entranceTestFactorsDetail;
        this.documentEvaluationScore = documentEvaluationScore;
    }
}
