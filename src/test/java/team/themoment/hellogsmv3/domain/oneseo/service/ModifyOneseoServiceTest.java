package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.CANDIDATE;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Major.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.GENERAL;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.SPECIAL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.DesiredMajors;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestFactorsDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.MiddleSchoolAchievementRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoPrivacyDetailRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.ScreeningChangeHistoryRepository;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda.LambdaScoreCalculatorClient;
import team.themoment.sdk.exception.ExpectedException;

@DisplayName("ModifyOneseoService 클래스의")
class ModifyOneseoServiceTest {

    @Mock
    private OneseoRepository oneseoRepository;
    @Mock
    private OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;
    @Mock
    private MiddleSchoolAchievementRepository middleSchoolAchievementRepository;
    @Mock
    private ScreeningChangeHistoryRepository screeningChangeHistoryRepository;
    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;
    @Mock
    private EntranceTestFactorsDetailRepository entranceTestFactorsDetailRepository;
    @Mock
    private OneseoService oneseoService;
    @Mock
    private LambdaScoreCalculatorClient lambdaScoreCalculatorClient;

    @InjectMocks
    private ModifyOneseoService modifyOneseoService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;

        List<Integer> achievement = Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5);
        List<String> generalSubjects = Arrays.asList("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어");
        List<String> newSubjects = List.of("프로그래밍");
        List<Integer> artsPhysicalAchievement = Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5);
        List<String> artsPhysicalSubjects = Arrays.asList("체육", "미술", "음악");
        List<Integer> absentDays = Arrays.asList(0, 0, 0);
        List<Integer> attendanceDays = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0);
        List<Integer> volunteerTime = Arrays.asList(2, 0, 10);
        String liberalSystem = "자유학기제";
        String freeSemester = "3-1";

        MiddleSchoolAchievementReqDto middleSchoolAchievementReqDto = MiddleSchoolAchievementReqDto.builder()
                .achievement1_2(null).achievement2_1(achievement).achievement2_2(achievement)
                .achievement3_1(achievement).achievement3_2(null).generalSubjects(generalSubjects)
                .newSubjects(newSubjects).artsPhysicalAchievement(artsPhysicalAchievement)
                .artsPhysicalSubjects(artsPhysicalSubjects).absentDays(absentDays).attendanceDays(attendanceDays)
                .volunteerTime(volunteerTime).liberalSystem(liberalSystem).freeSemester(freeSemester).build();

        String guardianName = "김보호";
        String guardianPhoneNumber = "01000000001";
        String relationshipWithGuardian = "모";
        String profileImg = "https://abc";
        String address = "광주광역시 광산구 송정동 상무대로 312";
        String detailAddress = "101동 1404호";
        GraduationType graduationType = CANDIDATE;
        String schoolTeacherName = "김선생";
        String schoolTeacherPhoneNumber = "01000000002";
        Major firstDesiredMajor = SW;
        Major secondDesiredMajor = AI;
        Major thirdDesiredMajor = IOT;
        String schoolName = "금호중앙중학교";
        String schoolAddress = "광주 어딘가";
        Screening screening = GENERAL;
        String graduationDate = "2020-02";
        String studentNumber = "30508";
        DesiredMajors desiredMajors = DesiredMajors.builder().firstDesiredMajor(firstDesiredMajor)
                .secondDesiredMajor(secondDesiredMajor).thirdDesiredMajor(thirdDesiredMajor).build();

        OneseoReqDto oneseoReqDto = new OneseoReqDto(guardianName,
                guardianPhoneNumber,
                relationshipWithGuardian,
                profileImg,
                address,
                detailAddress,
                graduationType,
                schoolTeacherName,
                schoolTeacherPhoneNumber,
                firstDesiredMajor,
                secondDesiredMajor,
                thirdDesiredMajor,
                middleSchoolAchievementReqDto,
                schoolName,
                schoolAddress,
                screening,
                graduationDate,
                studentNumber);

        @Nested
        @DisplayName("유효한 회원 ID와 요청 데이터가 주어지면")
        class Context_with_valid_member_id_and_request_data {

            @Test
            @DisplayName("기존의 Oneseo, OneseoPrivacyDetail, MiddleSchoolAchievement 엔티티를 수정하고 저장한다")
            void it_modifies_and_saves_existing_entities() {
                Member existingMember = mock(Member.class);

                Oneseo oneseo = Oneseo.builder().id(1L).member(existingMember).desiredMajors(desiredMajors)
                        .entranceTestResult(EntranceTestResult.builder().firstTestPassYn(null).build())
                        .wantedScreeningChangeHistory(new ArrayList<>()).build();

                OneseoPrivacyDetail oneseoPrivacyDetail = OneseoPrivacyDetail.builder().id(1L).oneseo(oneseo)
                        .graduationType(CANDIDATE).build();

                MiddleSchoolAchievement middleSchoolAchievement = MiddleSchoolAchievement.builder().id(1L)
                        .oneseo(oneseo).build();

                CalculatedScoreResDto mockCalculatedScore = mock(CalculatedScoreResDto.class);

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                given(oneseoPrivacyDetailRepository.findByOneseo(oneseo)).willReturn(oneseoPrivacyDetail);
                given(middleSchoolAchievementRepository.findByOneseo(oneseo)).willReturn(middleSchoolAchievement);
                given(lambdaScoreCalculatorClient.calculateScore(any(LambdaScoreCalculatorReqDto.class)))
                        .willReturn(mockCalculatedScore);
                given(entranceTestResultRepository.findByOneseo(any(Oneseo.class))).willReturn(null);

                modifyOneseoService.execute(oneseoReqDto, memberId);
                ArgumentCaptor<Oneseo> oneseoCaptor = ArgumentCaptor.forClass(Oneseo.class);
                ArgumentCaptor<OneseoPrivacyDetail> oneseoPrivacyDetailCaptor = ArgumentCaptor
                        .forClass(OneseoPrivacyDetail.class);
                ArgumentCaptor<MiddleSchoolAchievement> middleSchoolAchievementCaptor = ArgumentCaptor
                        .forClass(MiddleSchoolAchievement.class);

                verify(oneseoRepository).save(oneseoCaptor.capture());
                verify(oneseoPrivacyDetailRepository).save(oneseoPrivacyDetailCaptor.capture());
                verify(middleSchoolAchievementRepository).save(middleSchoolAchievementCaptor.capture());
                verify(lambdaScoreCalculatorClient).calculateScore(any(LambdaScoreCalculatorReqDto.class));
                verify(entranceTestFactorsDetailRepository).save(any(EntranceTestFactorsDetail.class));
                verify(entranceTestResultRepository).save(any(EntranceTestResult.class));

                Oneseo capturedOneseo = oneseoCaptor.getValue();
                OneseoPrivacyDetail capturedPrivacyDetail = oneseoPrivacyDetailCaptor.getValue();
                MiddleSchoolAchievement capturedAchievement = middleSchoolAchievementCaptor.getValue();

                assertEquals(oneseo.getId(), capturedOneseo.getId());
                assertEquals(firstDesiredMajor, capturedOneseo.getDesiredMajors().getFirstDesiredMajor());
                assertEquals(secondDesiredMajor, capturedOneseo.getDesiredMajors().getSecondDesiredMajor());
                assertEquals(thirdDesiredMajor, capturedOneseo.getDesiredMajors().getThirdDesiredMajor());
                assertEquals(screening, capturedOneseo.getWantedScreening());

                assertEquals(oneseoPrivacyDetail.getId(), capturedPrivacyDetail.getId());
                assertEquals(oneseoPrivacyDetail.getOneseo().getId(), capturedPrivacyDetail.getOneseo().getId());
                assertEquals(graduationType, capturedPrivacyDetail.getGraduationType());
                assertEquals(graduationDate, capturedPrivacyDetail.getGraduationDate());
                assertEquals(address, capturedPrivacyDetail.getAddress());
                assertEquals(detailAddress, capturedPrivacyDetail.getDetailAddress());
                assertEquals(profileImg, capturedPrivacyDetail.getProfileImg());
                assertEquals(guardianName, capturedPrivacyDetail.getGuardianName());
                assertEquals(guardianPhoneNumber, capturedPrivacyDetail.getGuardianPhoneNumber());
                assertEquals(relationshipWithGuardian, capturedPrivacyDetail.getRelationshipWithGuardian());
                assertEquals(schoolAddress, capturedPrivacyDetail.getSchoolAddress());
                assertEquals(schoolName, capturedPrivacyDetail.getSchoolName());
                assertEquals(schoolTeacherName, capturedPrivacyDetail.getSchoolTeacherName());
                assertEquals(schoolTeacherPhoneNumber, capturedPrivacyDetail.getSchoolTeacherPhoneNumber());

                assertEquals(middleSchoolAchievement.getId(), capturedAchievement.getId());
                assertEquals(middleSchoolAchievement.getOneseo().getId(), capturedAchievement.getOneseo().getId());
                assertEquals(achievement, capturedAchievement.getAchievement1_2());
                assertEquals(achievement, capturedAchievement.getAchievement2_1());
                assertEquals(achievement, capturedAchievement.getAchievement2_2());
                assertEquals(achievement, capturedAchievement.getAchievement3_1());
                assertNull(capturedAchievement.getAchievement3_2());
                assertEquals(generalSubjects, capturedAchievement.getGeneralSubjects());
                assertEquals(newSubjects, capturedAchievement.getNewSubjects());
                assertEquals(artsPhysicalAchievement, capturedAchievement.getArtsPhysicalAchievement());
                assertEquals(artsPhysicalSubjects, capturedAchievement.getArtsPhysicalSubjects());
                assertEquals(absentDays, capturedAchievement.getAbsentDays());
                assertEquals(attendanceDays, capturedAchievement.getAttendanceDays());
                assertEquals(volunteerTime, capturedAchievement.getVolunteerTime());
                assertEquals(liberalSystem, capturedAchievement.getLiberalSystem());
                assertEquals(freeSemester, capturedAchievement.getFreeSemester());
                assertNull(capturedAchievement.getGedAvgScore());
            }

            @Test
            @DisplayName("전형이 변경되었다면 히스토리를 남긴다")
            void it_change_screening_entity_save() {
                Screening beforeScreening = SPECIAL;
                Member existingMember = mock(Member.class);
                Oneseo oneseo = Oneseo.builder().id(1L).member(existingMember).wantedScreening(beforeScreening)
                        .desiredMajors(desiredMajors)
                        .entranceTestResult(EntranceTestResult.builder().firstTestPassYn(null).build())
                        .wantedScreeningChangeHistory(new ArrayList<>()).build();

                OneseoPrivacyDetail existingPrivacyDetail = mock(OneseoPrivacyDetail.class);
                MiddleSchoolAchievement existingAchievement = mock(MiddleSchoolAchievement.class);

                given(oneseoService.findWithMemberByMemberIdOrThrow(memberId)).willReturn(oneseo);
                given(oneseoPrivacyDetailRepository.findByOneseo(oneseo)).willReturn(existingPrivacyDetail);
                given(middleSchoolAchievementRepository.findByOneseo(oneseo)).willReturn(existingAchievement);
                given(entranceTestResultRepository.findByOneseo(any(Oneseo.class))).willReturn(null);
                CalculatedScoreResDto mockCalculatedScore = mock(CalculatedScoreResDto.class);
                given(lambdaScoreCalculatorClient.calculateScore(any(LambdaScoreCalculatorReqDto.class)))
                        .willReturn(mockCalculatedScore);

                modifyOneseoService.execute(oneseoReqDto, memberId);
                ArgumentCaptor<WantedScreeningChangeHistory> screeningChangeHistoryArgumentCaptor = ArgumentCaptor
                        .forClass(WantedScreeningChangeHistory.class);

                verify(screeningChangeHistoryRepository).save(screeningChangeHistoryArgumentCaptor.capture());
                verify(lambdaScoreCalculatorClient).calculateScore(any(LambdaScoreCalculatorReqDto.class));

                WantedScreeningChangeHistory capturedScreeningChangeHistory = screeningChangeHistoryArgumentCaptor
                        .getValue();

                assertEquals(beforeScreening, capturedScreeningChangeHistory.getBeforeScreening());
                assertEquals(GENERAL, capturedScreeningChangeHistory.getAfterScreening());
            }
        }

        @Nested
        @DisplayName("회원 ID가 유효하지 않으면")
        class Context_with_invalid_member_id {

            @BeforeEach
            void setUp() {
                willThrow(
                        new ExpectedException("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, HttpStatus.BAD_REQUEST))
                        .given(oneseoService).findWithMemberByMemberIdOrThrow(memberId);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_an_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyOneseoService.execute(oneseoReqDto, memberId));
                assertEquals("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("기존의 Oneseo 엔티티가 존재하지 않으면")
        class Context_with_no_existing_oneseo {

            @BeforeEach
            void setUp() {
                willThrow(
                        new ExpectedException("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, HttpStatus.BAD_REQUEST))
                        .given(oneseoService).findWithMemberByMemberIdOrThrow(memberId);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_an_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> modifyOneseoService.execute(oneseoReqDto, memberId));
                assertEquals("해당 지원자의 원서를 찾을 수 없습니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }
}
