package team.themoment.hellogsmv3.domain.oneseo.entity;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

@Getter
@Entity
@Table(name = "tb_oneseo", indexes = {
        @Index(name = "idx_applied_screening_and_real_oneseo_arrived_yn", columnList = "applied_screening, real_oneseo_arrived_yn"),
        @Index(name = "idx_real_oneseo_arrived_yn", columnList = "real_oneseo_arrived_yn")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicUpdate
public class Oneseo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oneseo_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToOne(mappedBy = "oneseo", cascade = CascadeType.ALL, orphanRemoval = true)
    private EntranceTestResult entranceTestResult;

    @OneToOne(mappedBy = "oneseo", cascade = CascadeType.ALL, orphanRemoval = true)
    private OneseoPrivacyDetail oneseoPrivacyDetail;

    @OneToOne(mappedBy = "oneseo", cascade = CascadeType.ALL, orphanRemoval = true)
    private MiddleSchoolAchievement middleSchoolAchievement;

    @Builder.Default
    @OneToMany(mappedBy = "oneseo", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WantedScreeningChangeHistory> wantedScreeningChangeHistory = new ArrayList<>();

    @Column(name = "oneseo_submit_code", unique = true)
    private String oneseoSubmitCode;

    @Column(name = "examination_number", length = 4, unique = true)
    private String examinationNumber;

    @Column(name = "pass_yn")
    @Enumerated(EnumType.STRING)
    private YesNo passYn;

    @NotNull
    @Embedded
    protected DesiredMajors desiredMajors;

    @Enumerated(EnumType.STRING)
    @Column(name = "real_oneseo_arrived_yn", nullable = false)
    private YesNo realOneseoArrivedYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "wanted_screening", nullable = false)
    private Screening wantedScreening;

    @Enumerated(EnumType.STRING)
    @Column(name = "applied_screening")
    private Screening appliedScreening;

    @Enumerated(EnumType.STRING)
    @Column(name = "entrance_intention_yn")
    private YesNo entranceIntentionYn;

    @Enumerated(EnumType.STRING)
    @Column(name = "decided_major")
    private Major decidedMajor;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "oneseo_edit_status", nullable = false)
    private OneseoEditStatus oneseoEditStatus = OneseoEditStatus.NONE;

    public void addWantedScreeningChangeHistory(WantedScreeningChangeHistory wantedScreeningChangeHistory) {
        this.wantedScreeningChangeHistory.add(wantedScreeningChangeHistory);
    }

    public void modifyOneseoPrivacyDetail(OneseoPrivacyDetail oneseoPrivacyDetail) {
        this.oneseoPrivacyDetail = oneseoPrivacyDetail;
    }

    public void modifyMiddleSchoolAchievement(MiddleSchoolAchievement middleSchoolAchievement) {
        this.middleSchoolAchievement = middleSchoolAchievement;
    }

    public void modifyEntranceTestResult(EntranceTestResult entranceTestResult) {
        this.entranceTestResult = entranceTestResult;
    }

    public Oneseo setOneseoSubmitCode(String submitCode) {
        this.oneseoSubmitCode = submitCode;

        return this;
    }

    public void modifyEntranceIntentionYn(YesNo yn) {
        this.entranceIntentionYn = yn;
    }

    public void switchRealOneseoArrivedYn() {
        this.realOneseoArrivedYn = this.realOneseoArrivedYn == YES ? NO : YES;
    }

    public void requestEditPermit() {
        this.oneseoEditStatus = OneseoEditStatus.REQUESTED;
    }

    public void approveEditPermit() {
        this.oneseoEditStatus = OneseoEditStatus.APPROVED;
    }

    public void revokeEditPermit() {
        this.oneseoEditStatus = OneseoEditStatus.NONE;
    }
}
