package team.themoment.hellogsmv3.domain.oneseo.entity;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import lombok.*;
import team.themoment.hellogsmv3.global.common.converter.IntegerListConverter;
import team.themoment.hellogsmv3.global.common.converter.StringListConverter;

@Getter
@Entity
@Table(name = "tb_middle_school_achievement")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicUpdate
public class MiddleSchoolAchievement {

    @Id
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oneseo_id")
    private Oneseo oneseo;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "achievement_1_2")
    private List<Integer> achievement1_2;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "achievement_2_1")
    private List<Integer> achievement2_1;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "achievement_2_2")
    private List<Integer> achievement2_2;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "achievement_3_1")
    private List<Integer> achievement3_1;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "achievement_3_2")
    private List<Integer> achievement3_2;

    @Convert(converter = StringListConverter.class)
    @Column(name = "general_subjects")
    private List<String> generalSubjects;

    @Convert(converter = StringListConverter.class)
    @Column(name = "new_subjects")
    private List<String> newSubjects;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "arts_physical_achievement")
    private List<Integer> artsPhysicalAchievement;

    @Convert(converter = StringListConverter.class)
    @Column(name = "arts_physical_subjects")
    private List<String> artsPhysicalSubjects;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "absent_days")
    private List<Integer> absentDays;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "attendance_days")
    private List<Integer> attendanceDays;

    @Convert(converter = IntegerListConverter.class)
    @Column(name = "volunteer_time")
    private List<Integer> volunteerTime;

    @Column(name = "liberal_system")
    private String liberalSystem;

    @Column(name = "free_semester")
    private String freeSemester;

    @Column(name = "ged_avg_score", precision = 5, scale = 2)
    private BigDecimal gedAvgScore;
}
