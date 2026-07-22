package team.themoment.hellogsmv3.domain.member.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import lombok.*;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthReferrerType;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;

@Getter
@Builder
@Entity
@Table(name = "tb_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_referrer_type", nullable = false)
    private AuthReferrerType authReferrerType;

    @Column(name = "name")
    private String name;

    @Column(name = "birth")
    private LocalDate birth;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "sex")
    private Sex sex;

    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Role role;

    @CreatedDate
    @Column(name = "created_time", updatable = false, nullable = false)
    private LocalDateTime createdTime;

    @LastModifiedDate
    @Column(name = "updated_time", nullable = false)
    private LocalDateTime updatedTime;

    public static Member buildMemberWithOauthInfo(String email, AuthReferrerType authRefType) {
        return Member.builder().email(email).authReferrerType(authRefType).build();
    }

    public Member modifyMember(String name, LocalDate birth, String phoneNumber, Sex sex) {
        this.name = name;
        this.birth = birth;
        this.phoneNumber = phoneNumber;
        this.sex = sex;

        return this;
    }

    public void modifyMemberRole(Role role) {
        this.role = role;
    }

    @PrePersist
    private void prePersist() {
        this.role = this.role == null ? Role.UNAUTHENTICATED : this.role;
    }
}
