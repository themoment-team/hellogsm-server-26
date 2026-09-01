package team.themoment.hellogsmv3.domain.oneseo.entity;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import lombok.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;

@Getter
@Entity
@Table(name = "tb_oneseo_privacy_detail")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicUpdate
public class OneseoPrivacyDetail {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oneseo_id")
    private Oneseo oneseo;

    @Enumerated(EnumType.STRING)
    @Column(name = "graduation_type", nullable = false)
    private GraduationType graduationType;

    @Column(name = "graduation_date", nullable = false, columnDefinition = "CHAR(7)")
    private String graduationDate;

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "detail_address", nullable = false)
    private String detailAddress;

    @Column(name = "profile_img", nullable = false)
    private String profileImg;

    @Column(name = "guardian_name", nullable = false)
    private String guardianName;

    @Column(name = "guardian_phone_number", nullable = false)
    private String guardianPhoneNumber;

    @Column(name = "relationship_with_guardian", nullable = false)
    private String relationshipWithGuardian;

    @Column(name = "school_address")
    private String schoolAddress;

    @Column(name = "school_name")
    private String schoolName;

    @Column(name = "school_teacher_name")
    private String schoolTeacherName;

    @Column(name = "school_teacher_phone_number")
    private String schoolTeacherPhoneNumber;

    @Column(name = "student_number")
    private String studentNumber;
}
