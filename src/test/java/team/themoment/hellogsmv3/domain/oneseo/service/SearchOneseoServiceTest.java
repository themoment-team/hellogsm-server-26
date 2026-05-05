package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoEditStatusTag;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.TestResultTag;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseoPageInfoDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseoResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseosResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.ScreeningCategory;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

@DisplayName("SearchOneseoService 클래스의")
class SearchOneseoServiceTest {

    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private SearchOneseoService searchOneseoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final int page = 0;
        private final int size = 3;
        private final TestResultTag testResultTag = TestResultTag.ALL;
        private final ScreeningCategory screeningTag = ScreeningCategory.GENERAL;
        private final YesNo isSubmitted = YES;
        private final String keyword = "최장우";

        @Nested
        @DisplayName("검색 결과가 없을 때")
        class Context_with_no_search_results {

            @BeforeEach
            void setUp() {
                Pageable pageable = PageRequest.of(page, size);
                Page<SearchOneseoResDto> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

                given(oneseoRepository.findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(keyword,
                        screeningTag,
                        isSubmitted,
                        testResultTag,
                        null,
                        pageable)).willReturn(emptyPage);
            }

            @Test
            @DisplayName("빈 결과를 반환한다")
            void it_returns_empty_result() {
                SearchOneseosResDto result = searchOneseoService
                        .execute(page, size, testResultTag, screeningTag, isSubmitted, keyword, null);

                assertEquals(0, result.info().totalElements());
                assertEquals(0, result.info().totalPages());
                assertEquals(0, result.oneseos().size());
            }
        }

        @Nested
        @DisplayName("필터링 조건에 따라")
        class Context_with_filtering {

            private Member member;
            private Oneseo oneseo;
            private OneseoPrivacyDetail oneseoPrivacyDetail;
            private EntranceTestResult entranceTestResult;

            @BeforeEach
            void setUp() {
                Pageable pageable = PageRequest.of(page, size);
                member = buildMember();
                oneseo = buildOneseo();

                oneseoPrivacyDetail = buildOneseoPrivacyDetail();
                entranceTestResult = buildEntranceTestResult();

                SearchOneseoResDto searchOneseoResDto = buildSearchOneseoDto(member,
                        oneseo,
                        oneseoPrivacyDetail,
                        entranceTestResult);
                Page<SearchOneseoResDto> oneseoPage = new PageImpl<>(List.of(searchOneseoResDto), pageable, 1);

                given(oneseoRepository.findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(keyword,
                        screeningTag,
                        isSubmitted,
                        testResultTag,
                        null,
                        pageable)).willReturn(oneseoPage);
            }

            @Test
            @DisplayName("적절한 데이터를 반환한다")
            void it_returns_filtered_results() {
                SearchOneseosResDto result = searchOneseoService
                        .execute(page, size, testResultTag, screeningTag, isSubmitted, keyword, null);

                SearchOneseoPageInfoDto searchOneseoPageInfoDto = result.info();
                assertEquals(1, searchOneseoPageInfoDto.totalElements());
                assertEquals(1, searchOneseoPageInfoDto.totalPages());

                SearchOneseoResDto searchOneseoResDto = result.oneseos().getFirst();
                assertEquals(member.getId(), searchOneseoResDto.memberId());
                assertEquals(oneseo.getOneseoSubmitCode(), searchOneseoResDto.submitCode());
                assertEquals(oneseo.getRealOneseoArrivedYn(), searchOneseoResDto.realOneseoArrivedYn());
                assertEquals(member.getName(), searchOneseoResDto.name());
                assertEquals(oneseo.getWantedScreening(), searchOneseoResDto.screening());
                assertEquals(oneseoPrivacyDetail.getSchoolName(), searchOneseoResDto.schoolName());
                assertEquals(member.getPhoneNumber(), searchOneseoResDto.phoneNumber());
                assertEquals(oneseoPrivacyDetail.getGuardianPhoneNumber(), searchOneseoResDto.guardianPhoneNumber());
                assertEquals(oneseoPrivacyDetail.getSchoolTeacherPhoneNumber(),
                        searchOneseoResDto.schoolTeacherPhoneNumber());
                assertEquals(entranceTestResult.getFirstTestPassYn(), searchOneseoResDto.firstTestPassYn());
                assertEquals(entranceTestResult.getCompetencyEvaluationScore(),
                        searchOneseoResDto.competencyEvaluationScore());
                assertEquals(entranceTestResult.getInterviewScore(), searchOneseoResDto.interviewScore());
                assertEquals(entranceTestResult.getSecondTestPassYn(), searchOneseoResDto.secondTestPassYn());
            }
        }

        @Nested
        @DisplayName("status=ANY_EDIT이 주어진 경우")
        class Context_with_status_any_edit {

            private Member member;
            private Oneseo oneseo;

            @BeforeEach
            void setUp() {
                Pageable pageable = PageRequest.of(page, size);
                member = buildMember();
                oneseo = buildOneseo();
                OneseoPrivacyDetail oneseoPrivacyDetail = buildOneseoPrivacyDetail();
                EntranceTestResult entranceTestResult = buildEntranceTestResult();

                SearchOneseoResDto searchOneseoResDto = buildSearchOneseoDto(member,
                        oneseo,
                        oneseoPrivacyDetail,
                        entranceTestResult);
                Page<SearchOneseoResDto> oneseoPage = new PageImpl<>(List.of(searchOneseoResDto), pageable, 1);

                given(oneseoRepository.findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(keyword,
                        screeningTag,
                        isSubmitted,
                        testResultTag,
                        OneseoEditStatusTag.ANY_EDIT,
                        pageable)).willReturn(oneseoPage);
            }

            @Test
            @DisplayName("ANY_EDIT 필터가 repository로 올바르게 전달된다")
            void it_passes_any_edit_status_filter_to_repository() {
                SearchOneseosResDto result = searchOneseoService.execute(page,
                        size,
                        testResultTag,
                        screeningTag,
                        isSubmitted,
                        keyword,
                        OneseoEditStatusTag.ANY_EDIT);

                assertEquals(1, result.info().totalElements());
                assertEquals(1, result.oneseos().size());
                assertEquals(member.getId(), result.oneseos().getFirst().memberId());
                assertEquals(oneseo.getOneseoSubmitCode(), result.oneseos().getFirst().submitCode());
            }
        }

        @Nested
        @DisplayName("status=REQUESTED가 주어진 경우")
        class Context_with_status_requested {

            private Member member;
            private Oneseo oneseo;

            @BeforeEach
            void setUp() {
                Pageable pageable = PageRequest.of(page, size);
                member = buildMember();
                oneseo = buildOneseo();
                OneseoPrivacyDetail oneseoPrivacyDetail = buildOneseoPrivacyDetail();
                EntranceTestResult entranceTestResult = buildEntranceTestResult();

                SearchOneseoResDto searchOneseoResDto = buildSearchOneseoDto(member,
                        oneseo,
                        oneseoPrivacyDetail,
                        entranceTestResult);
                Page<SearchOneseoResDto> oneseoPage = new PageImpl<>(List.of(searchOneseoResDto), pageable, 1);

                given(oneseoRepository.findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(keyword,
                        screeningTag,
                        isSubmitted,
                        testResultTag,
                        OneseoEditStatusTag.REQUESTED,
                        pageable)).willReturn(oneseoPage);
            }

            @Test
            @DisplayName("REQUESTED 필터가 repository로 올바르게 전달된다")
            void it_passes_requested_status_filter_to_repository() {
                SearchOneseosResDto result = searchOneseoService.execute(page,
                        size,
                        testResultTag,
                        screeningTag,
                        isSubmitted,
                        keyword,
                        OneseoEditStatusTag.REQUESTED);

                assertEquals(1, result.info().totalElements());
                assertEquals(1, result.oneseos().size());
                assertEquals(member.getId(), result.oneseos().getFirst().memberId());
                assertEquals(oneseo.getOneseoSubmitCode(), result.oneseos().getFirst().submitCode());
            }
        }

        @Nested
        @DisplayName("status=APPROVED가 주어진 경우")
        class Context_with_status_approved {

            private Member member;
            private Oneseo oneseo;

            @BeforeEach
            void setUp() {
                Pageable pageable = PageRequest.of(page, size);
                member = buildMember();
                oneseo = buildOneseo();
                OneseoPrivacyDetail oneseoPrivacyDetail = buildOneseoPrivacyDetail();
                EntranceTestResult entranceTestResult = buildEntranceTestResult();

                SearchOneseoResDto searchOneseoResDto = buildSearchOneseoDto(member,
                        oneseo,
                        oneseoPrivacyDetail,
                        entranceTestResult);
                Page<SearchOneseoResDto> oneseoPage = new PageImpl<>(List.of(searchOneseoResDto), pageable, 1);

                given(oneseoRepository.findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(keyword,
                        screeningTag,
                        isSubmitted,
                        testResultTag,
                        OneseoEditStatusTag.APPROVED,
                        pageable)).willReturn(oneseoPage);
            }

            @Test
            @DisplayName("APPROVED 필터가 repository로 올바르게 전달된다")
            void it_passes_approved_status_filter_to_repository() {
                SearchOneseosResDto result = searchOneseoService.execute(page,
                        size,
                        testResultTag,
                        screeningTag,
                        isSubmitted,
                        keyword,
                        OneseoEditStatusTag.APPROVED);

                assertEquals(1, result.info().totalElements());
                assertEquals(1, result.oneseos().size());
                assertEquals(member.getId(), result.oneseos().getFirst().memberId());
                assertEquals(oneseo.getOneseoSubmitCode(), result.oneseos().getFirst().submitCode());
            }
        }
    }

    private OneseoPrivacyDetail buildOneseoPrivacyDetail() {
        return OneseoPrivacyDetail.builder().schoolName("양산중학교").guardianPhoneNumber("01012345678")
                .schoolTeacherPhoneNumber("01087654321").build();
    }

    private Member buildMember() {
        return Member.builder().id(1L).name("최장우").phoneNumber("01044448888").build();
    }

    private Oneseo buildOneseo() {
        return Oneseo.builder().id(1L).oneseoSubmitCode("A-1").realOneseoArrivedYn(YES).wantedScreening(GENERAL)
                .build();
    }

    private SearchOneseoResDto buildSearchOneseoDto(Member member,
            Oneseo oneseo,
            OneseoPrivacyDetail oneseoPrivacyDetail,
            EntranceTestResult entranceTestResult) {
        return SearchOneseoResDto.builder().memberId(member.getId()).submitCode(oneseo.getOneseoSubmitCode())
                .realOneseoArrivedYn(oneseo.getRealOneseoArrivedYn()).name(member.getName())
                .screening(oneseo.getWantedScreening()).schoolName(oneseoPrivacyDetail.getSchoolName())
                .phoneNumber(member.getPhoneNumber()).guardianPhoneNumber(oneseoPrivacyDetail.getGuardianPhoneNumber())
                .schoolTeacherPhoneNumber(oneseoPrivacyDetail.getSchoolTeacherPhoneNumber())
                .firstTestPassYn(entranceTestResult.getFirstTestPassYn())
                .competencyEvaluationScore(entranceTestResult.getCompetencyEvaluationScore())
                .interviewScore(entranceTestResult.getInterviewScore())
                .secondTestPassYn(entranceTestResult.getSecondTestPassYn()).build();
    }

    private EntranceTestResult buildEntranceTestResult() {
        return EntranceTestResult.builder().id(1L).firstTestPassYn(YES).competencyEvaluationScore(BigDecimal.TEN)
                .interviewScore(BigDecimal.TEN).secondTestPassYn(YES).build();
    }
}
