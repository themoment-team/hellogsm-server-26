package team.themoment.hellogsmv3.domain.oneseo.repository.custom.impl;

import static com.querydsl.core.types.ExpressionUtils.anyOf;
import static team.themoment.hellogsmv3.domain.member.entity.QMember.member;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QEntranceTestFactorsDetail.entranceTestFactorsDetail;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QEntranceTestResult.entranceTestResult;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QMiddleSchoolAchievement.middleSchoolAchievement;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QOneseo.oneseo;
import static team.themoment.hellogsmv3.domain.oneseo.entity.QOneseoPrivacyDetail.oneseoPrivacyDetail;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.YES;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.EnumPath;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.FoundMemberAndOneseoDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoEditStatusTag;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoSortBy;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.TestResultTag;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.AdmissionTicketsResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseoResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.OneseoEditStatus;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.ScreeningCategory;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.custom.CustomOneseoRepository;

@Repository
@RequiredArgsConstructor
public class CustomOneseoRepositoryImpl implements CustomOneseoRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AdmissionTicketsResDto> findAdmissionTickets() {
        return queryFactory
                .select(Projections.constructor(AdmissionTicketsResDto.class,
                        oneseo.member.name,
                        oneseo.member.birth,
                        oneseoPrivacyDetail.profileImg,
                        oneseoPrivacyDetail.schoolName,
                        oneseo.examinationNumber,
                        oneseo.oneseoSubmitCode))
                .from(oneseo).join(oneseo.member, member).join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail)
                .join(oneseo.entranceTestResult, entranceTestResult).where(entranceTestResult.firstTestPassYn.eq(YES))
                .fetch();
    }

    @Override
    public Integer findMaxSubmitCodeByScreening(ScreeningCategory screeningCategory) {
        return queryFactory.select(oneseo.oneseoSubmitCode.substring(2).castToNum(Integer.class)).from(oneseo)
                .where(oneseo.wantedScreening.in(Screening.findAllByScreeningCategory(screeningCategory)))
                .orderBy(oneseo.oneseoSubmitCode.substring(2).castToNum(Integer.class).desc()).fetchFirst();
    }

    @Override
    public Page<SearchOneseoResDto> findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(String keyword,
            ScreeningCategory screeningTag,
            YesNo isSubmitted,
            TestResultTag testResultTag,
            OneseoEditStatusTag status,
            OneseoSortBy sortBy,
            Sort.Direction sortDirection,
            Pageable pageable) {

        BooleanBuilder builder = createBooleanBuilder(keyword, screeningTag, isSubmitted, testResultTag, status);

        List<SearchOneseoResDto> oneseos = queryFactory
                .select(Projections.constructor(SearchOneseoResDto.class,
                        oneseo.member.id,
                        oneseo.oneseoSubmitCode,
                        oneseo.realOneseoArrivedYn,
                        oneseo.member.name,
                        oneseo.wantedScreening,
                        oneseo.oneseoPrivacyDetail.schoolName,
                        oneseo.member.phoneNumber,
                        oneseo.oneseoPrivacyDetail.guardianPhoneNumber,
                        oneseo.oneseoPrivacyDetail.schoolTeacherPhoneNumber,
                        oneseo.examinationNumber,
                        oneseo.entranceTestResult.firstTestPassYn,
                        oneseo.entranceTestResult.competencyEvaluationScore,
                        oneseo.entranceTestResult.interviewScore,
                        oneseo.entranceTestResult.secondTestPassYn,
                        oneseo.entranceIntentionYn,
                        oneseo.oneseoEditStatus))
                .from(oneseo).join(oneseo.member, member).join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail)
                .join(oneseo.entranceTestResult, entranceTestResult).where(builder)
                .orderBy(createOrderSpecifiers(sortBy, sortDirection)).offset(pageable.getOffset())
                .limit(pageable.getPageSize()).fetch();

        return PageableExecutionUtils.getPage(oneseos, pageable, () -> getTotalCount(builder));
    }

    /**
     * sortBy가 없으면 접수번호 내림차순으로 정렬합니다. offset 페이징 시 동점 지원자가 페이지 사이에서 중복·누락되지 않도록 접수번호
     * 오름차순, oneseo id 순으로 동점을 정렬합니다.
     */
    private OrderSpecifier<?>[] createOrderSpecifiers(OneseoSortBy sortBy, Sort.Direction sortDirection) {
        if (sortBy == null)
            return new OrderSpecifier<?>[]{submitCodePrefix().desc(), submitCodeNumber().desc(), oneseo.id.desc()};

        Order order = sortDirection == Sort.Direction.DESC ? Order.DESC : Order.ASC;
        List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>(createPrimaryOrderSpecifiers(sortBy, order));
        if (sortBy != OneseoSortBy.SUBMIT_CODE) {
            orderSpecifiers.add(submitCodePrefix().asc());
            orderSpecifiers.add(submitCodeNumber().asc());
        }
        orderSpecifiers.add(oneseo.id.asc());

        return orderSpecifiers.toArray(OrderSpecifier<?>[]::new);
    }

    private List<OrderSpecifier<?>> createPrimaryOrderSpecifiers(OneseoSortBy sortBy, Order order) {
        return switch (sortBy) {
            case SUBMIT_CODE -> List.of(new OrderSpecifier<>(order, submitCodePrefix()),
                    new OrderSpecifier<>(order, submitCodeNumber()));
            case NAME -> List.of(new OrderSpecifier<>(order, member.name));
            case SCHOOL_NAME -> List.of(
                    missingLast(oneseoPrivacyDetail.schoolName.isNull().or(oneseoPrivacyDetail.schoolName.isEmpty())),
                    new OrderSpecifier<>(order, oneseoPrivacyDetail.schoolName));
            case SCREENING -> List.of(new OrderSpecifier<>(order, screeningRank()));
            case COMPETENCY_SCORE -> List.of(missingLast(entranceTestResult.competencyEvaluationScore.isNull()),
                    new OrderSpecifier<>(order, entranceTestResult.competencyEvaluationScore));
            case INTERVIEW_SCORE -> List.of(missingLast(entranceTestResult.interviewScore.isNull()),
                    new OrderSpecifier<>(order, entranceTestResult.interviewScore));
            case REAL_ONESEO_ARRIVED -> yesNoOrderSpecifiers(oneseo.realOneseoArrivedYn, order);
            case FIRST_TEST_PASS -> yesNoOrderSpecifiers(entranceTestResult.firstTestPassYn, order);
            case SECOND_TEST_PASS -> yesNoOrderSpecifiers(entranceTestResult.secondTestPassYn, order);
            case ENTRANCE_INTENTION -> yesNoOrderSpecifiers(oneseo.entranceIntentionYn, order);
        };
    }

    private StringExpression submitCodePrefix() {
        return oneseo.oneseoSubmitCode.substring(0, 1);
    }

    private NumberExpression<Integer> submitCodeNumber() {
        return oneseo.oneseoSubmitCode.substring(2).castToNum(Integer.class);
    }

    /**
     * 정렬 방향과 관계없이 값이 없는 지원자를 맨 뒤로 보냅니다.
     */
    private OrderSpecifier<Integer> missingLast(BooleanExpression isMissing) {
        return new CaseBuilder().when(isMissing).then(1).otherwise(0).asc();
    }

    /**
     * enum 선언 순서가 아닌 일반 → 특별 → 정원 외(국가보훈 → 특례입학) 순으로 정렬합니다.
     */
    private NumberExpression<Integer> screeningRank() {
        return new CaseBuilder().when(oneseo.wantedScreening.eq(Screening.GENERAL)).then(0)
                .when(oneseo.wantedScreening.eq(Screening.SPECIAL)).then(1)
                .when(oneseo.wantedScreening.eq(Screening.EXTRA_VETERANS)).then(2).otherwise(3);
    }

    /**
     * 오름차순일 때 YES가 NO보다 먼저 오도록 정렬합니다.
     */
    private List<OrderSpecifier<?>> yesNoOrderSpecifiers(EnumPath<YesNo> yesNo, Order order) {
        NumberExpression<Integer> yesNoRank = new CaseBuilder().when(yesNo.eq(YES)).then(0).otherwise(1);
        return List.of(missingLast(yesNo.isNull()), new OrderSpecifier<>(order, yesNoRank));
    }

    @Override
    public List<Oneseo> findAllByScreeningWithAllDetails(Screening screening) {
        boolean isExistAppliedScreening = queryFactory.selectOne().from(oneseo)
                .where(oneseo.appliedScreening.isNotNull()).fetchFirst() != null;

        return queryFactory.selectFrom(oneseo).join(oneseo.member, member).fetchJoin()
                .join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail).fetchJoin()
                .join(oneseo.entranceTestResult, entranceTestResult).fetchJoin()
                .join(entranceTestResult.entranceTestFactorsDetail, entranceTestFactorsDetail).fetchJoin()
                .leftJoin(oneseo.middleSchoolAchievement, middleSchoolAchievement).fetchJoin()
                .where((isExistAppliedScreening
                        ? oneseo.appliedScreening.eq(screening)
                        : oneseo.wantedScreening.eq(screening))
                        .and(entranceTestResult.firstTestPassYn.eq(YES))
                        .and(entranceTestResult.secondTestPassYn.eq(YES)
                                .or(entranceTestResult.secondTestPassYn.isNull())))
                .orderBy(oneseo.oneseoSubmitCode.substring(0, 1).asc(),
                        oneseo.oneseoSubmitCode.substring(2).castToNum(Integer.class).asc())
                .fetch();
    }

    @Override
    public List<Oneseo> findAllFailedWithAllDetails() {
        return queryFactory.selectFrom(oneseo).join(oneseo.member, member).fetchJoin()
                .join(oneseo.oneseoPrivacyDetail, oneseoPrivacyDetail).fetchJoin()
                .join(oneseo.entranceTestResult, entranceTestResult).fetchJoin()
                .join(entranceTestResult.entranceTestFactorsDetail, entranceTestFactorsDetail).fetchJoin()
                .leftJoin(oneseo.middleSchoolAchievement, middleSchoolAchievement).fetchJoin()
                .where(entranceTestResult.firstTestPassYn.eq(NO).or(entranceTestResult.secondTestPassYn.eq(NO)))
                .orderBy(oneseo.oneseoSubmitCode.substring(0, 1).asc(),
                        oneseo.oneseoSubmitCode.substring(2).castToNum(Integer.class).asc())
                .fetch();
    }

    private long getTotalCount(BooleanBuilder builder) {
        return queryFactory.select(oneseo.count()).from(oneseo).where(builder).fetchFirst();
    }

    private BooleanBuilder createBooleanBuilder(String keyword,
            ScreeningCategory screeningTag,
            YesNo isSubmitted,
            TestResultTag testResultTag,
            OneseoEditStatusTag status) {

        BooleanBuilder builder = new BooleanBuilder();
        applyKeyword(builder, keyword);
        applyScreeningTag(builder, screeningTag);
        applyIsSubmittedTag(builder, isSubmitted);
        applyTestResultTag(builder, testResultTag);
        applyEditStatusTag(builder, status);

        return builder;
    }

    private void applyEditStatusTag(BooleanBuilder builder, OneseoEditStatusTag status) {
        if (status == null)
            return;

        switch (status) {
            case ANY_EDIT ->
                builder.and(oneseo.oneseoEditStatus.in(OneseoEditStatus.REQUESTED, OneseoEditStatus.APPROVED));
            case REQUESTED -> builder.and(oneseo.oneseoEditStatus.eq(OneseoEditStatus.REQUESTED));
            case APPROVED -> builder.and(oneseo.oneseoEditStatus.eq(OneseoEditStatus.APPROVED));
        }
    }

    private void applyKeyword(BooleanBuilder builder, String keyword) {
        if (keyword == null)
            return;

        builder.and(anyOf(oneseo.member.name.like("%" + keyword + "%"),
                oneseoPrivacyDetail.schoolName.like("%" + keyword + "%"),
                oneseo.member.phoneNumber.like("%" + keyword + "%")));
    }

    private void applyScreeningTag(BooleanBuilder builder, ScreeningCategory screeningTag) {
        if (screeningTag == null)
            return;

        switch (screeningTag) {
            case GENERAL -> builder.and(oneseo.wantedScreening.eq(Screening.GENERAL));
            case SPECIAL -> builder.and(oneseo.wantedScreening.eq(Screening.SPECIAL));
            case EXTRA -> builder.andAnyOf(oneseo.wantedScreening.eq(Screening.EXTRA_ADMISSION),
                    oneseo.wantedScreening.eq(Screening.EXTRA_VETERANS));
        }
    }

    private void applyIsSubmittedTag(BooleanBuilder builder, YesNo isSubmitted) {
        if (isSubmitted == null)
            return;

        switch (isSubmitted) {
            case YES -> builder.and(oneseo.realOneseoArrivedYn.eq(YES));
            case NO -> builder.and(oneseo.realOneseoArrivedYn.eq(NO));
        }
    }

    private void applyTestResultTag(BooleanBuilder builder, TestResultTag testResultTag) {

        switch (testResultTag) {
            case FIRST_PASS -> builder.and(entranceTestResult.firstTestPassYn.eq(YES));
            case FINAL_PASS -> builder.and(entranceTestResult.secondTestPassYn.eq(YES));
            case FALL ->
                builder.andAnyOf(entranceTestResult.firstTestPassYn.eq(NO), entranceTestResult.secondTestPassYn.eq(NO));
        }
    }

    @Override
    public Map<String, EntranceTestResult> findEntranceTestResultByExaminationNumbersIn(
            List<String> examinationNumbers) {
        return queryFactory.selectFrom(entranceTestResult).select(oneseo.examinationNumber, entranceTestResult)
                .join(entranceTestResult.oneseo, oneseo).where(oneseo.examinationNumber.in(examinationNumbers)).fetch()
                .stream().collect(Collectors.toMap(tuple -> tuple.get(oneseo.examinationNumber),
                        tuple -> tuple.get(entranceTestResult)));
    }

    @Override
    public FoundMemberAndOneseoDto findMemberAndOneseoByMemberId(Long memberId) {
        Tuple result = queryFactory.select(member, oneseo).from(member).leftJoin(oneseo).on(oneseo.member.eq(member))
                .where(member.id.eq(memberId)).fetchOne();
        if (result == null)
            return new FoundMemberAndOneseoDto(null, null);

        return new FoundMemberAndOneseoDto(result.get(member), result.get(oneseo));
    }

}
