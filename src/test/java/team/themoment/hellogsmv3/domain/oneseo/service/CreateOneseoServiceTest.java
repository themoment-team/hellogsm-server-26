package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Major.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.*;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.service.MemberService;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.MiddleSchoolAchievementReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.OneseoReqDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.CalculatedScoreResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.MiddleSchoolAchievement;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.repository.*;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.dto.request.LambdaScoreCalculatorReqDto;
import team.themoment.hellogsmv3.global.thirdParty.feign.client.lambda.LambdaScoreCalculatorClient;
import team.themoment.sdk.exception.ExpectedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreateOneseoService 클래스의")
class CreateOneseoServiceTest {

    @Mock
    private OneseoRepository oneseoRepository;
    @Mock
    private OneseoPrivacyDetailRepository oneseoPrivacyDetailRepository;
    @Mock
    private MiddleSchoolAchievementRepository middleSchoolAchievementRepository;
    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;
    @Mock
    private MemberService memberService;
    @Mock
    private LambdaScoreCalculatorClient lambdaScoreCalculatorClient;

    @InjectMocks
    private CreateOneseoService createOneseoService;

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {

        private final Long memberId = 1L;
        private final OneseoReqDto reqDto = mock(OneseoReqDto.class);

        List<Integer> achievement = Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5);
        List<String> generalSubjects = Arrays.asList("국어", "도덕", "사회", "역사", "수학", "과학", "기술가정", "영어");
        List<String> newSubjects = List.of("프로그래밍");
        List<Integer> artsPhysicalAchievement = Arrays.asList(5, 5, 5, 5, 5, 5, 5, 5, 5);
        List<String> artsPhysicalSubjects = Arrays.asList("체육", "미술", "음악");
        List<Integer> absentDays = Arrays.asList(0, 0, 0);
        List<Integer> attendanceDays = Arrays.asList(0, 0, 0, 0, 0, 0, 0, 0, 0);
        List<Integer> volunteerTime = Arrays.asList(10, 0, 2);
        String liberalSystem = "자유학기제";
        String freeSemester = "1-1";

        MiddleSchoolAchievementReqDto middleSchoolAchievementReqDto = MiddleSchoolAchievementReqDto.builder()
                .achievement1_2(achievement).achievement2_1(achievement).achievement2_2(achievement)
                .achievement3_1(achievement).generalSubjects(generalSubjects).newSubjects(newSubjects)
                .artsPhysicalAchievement(artsPhysicalAchievement).artsPhysicalSubjects(artsPhysicalSubjects)
                .absentDays(absentDays).attendanceDays(attendanceDays).volunteerTime(volunteerTime)
                .liberalSystem(liberalSystem).freeSemester(freeSemester).build();

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

            @BeforeEach
            void setUp() {
                Member existingMember = mock(Member.class);
                CalculatedScoreResDto mockCalculatedScore = mock(CalculatedScoreResDto.class);

                given(memberService.findByIdForUpdateOrThrow(memberId)).willReturn(existingMember);
                given(oneseoRepository.existsByMember(existingMember)).willReturn(false);
                given(lambdaScoreCalculatorClient.calculateScore(any(LambdaScoreCalculatorReqDto.class)))
                        .willReturn(mockCalculatedScore);
                given(entranceTestResultRepository.findByOneseo(any(Oneseo.class))).willReturn(null);
            }

            @Test
            @DisplayName("새로운 Oneseo, OneseoPrivacyDetail, MiddleSchoolAchievement 엔티티를 생성하고 저장한다")
            void it_creates_and_saves_new_entities() {
                createOneseoService.execute(oneseoReqDto, memberId);

                ArgumentCaptor<Oneseo> oneseoCaptor = ArgumentCaptor.forClass(Oneseo.class);
                ArgumentCaptor<OneseoPrivacyDetail> oneseoPrivacyDetailCaptor = ArgumentCaptor
                        .forClass(OneseoPrivacyDetail.class);
                ArgumentCaptor<MiddleSchoolAchievement> middleSchoolAchievementCaptor = ArgumentCaptor
                        .forClass(MiddleSchoolAchievement.class);

                verify(oneseoRepository).save(oneseoCaptor.capture());
                verify(oneseoPrivacyDetailRepository).save(oneseoPrivacyDetailCaptor.capture());
                verify(middleSchoolAchievementRepository).save(middleSchoolAchievementCaptor.capture());

                Oneseo capturedOneseo = oneseoCaptor.getValue();
                OneseoPrivacyDetail capturedPrivacyDetail = oneseoPrivacyDetailCaptor.getValue();
                MiddleSchoolAchievement capturedAchievement = middleSchoolAchievementCaptor.getValue();

                assertEquals(firstDesiredMajor, capturedOneseo.getDesiredMajors().getFirstDesiredMajor());
                assertEquals(secondDesiredMajor, capturedOneseo.getDesiredMajors().getSecondDesiredMajor());
                assertEquals(thirdDesiredMajor, capturedOneseo.getDesiredMajors().getThirdDesiredMajor());
                assertEquals(NO, capturedOneseo.getRealOneseoArrivedYn());
                assertEquals(screening, capturedOneseo.getWantedScreening());

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

                assertEquals(achievement, capturedAchievement.getAchievement1_2());
                assertEquals(achievement, capturedAchievement.getAchievement2_1());
                assertEquals(achievement, capturedAchievement.getAchievement2_2());
                assertEquals(achievement, capturedAchievement.getAchievement3_1());
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
        }

        @Nested
        @DisplayName("회원 ID가 유효하지 않으면")
        class Context_with_invalid_member_id {

            @BeforeEach
            void setUp() {
                given(reqDto.graduationType()).willReturn(GRADUATE);

                willThrow(new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND))
                        .given(memberService).findByIdForUpdateOrThrow(memberId);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> createOneseoService.execute(reqDto, memberId));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("이미 Oneseo가 존재하면")
        class Context_with_existing_oneseo {

            @BeforeEach
            void setUp() {
                Member existingMember = mock(Member.class);

                given(reqDto.graduationType()).willReturn(GRADUATE);
                given(memberService.findByIdForUpdateOrThrow(memberId)).willReturn(existingMember);
                given(oneseoRepository.existsByMember(any(Member.class))).willReturn(true);
            }

            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> createOneseoService.execute(reqDto, memberId));

                assertEquals("이미 원서가 존재합니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }

        @Nested
        @DisplayName("유효하지 않은 교과 등급이 입력되었을 때")
        class Context_with_invalid_achievement {

            MiddleSchoolAchievementReqDto invalidMiddleSchoolAchievementReqDto = mock(
                    MiddleSchoolAchievementReqDto.class);

            @BeforeEach
            void setUp() {
                Member existingMember = mock(Member.class);

                given(reqDto.graduationType()).willReturn(GRADUATE);
                given(memberService.findByIdForUpdateOrThrow(memberId)).willReturn(existingMember);
                given(oneseoRepository.existsByMember(existingMember)).willReturn(false);
                given(reqDto.middleSchoolAchievement()).willReturn(invalidMiddleSchoolAchievementReqDto);
            }

            @Test
            @DisplayName("일반교과 등급이 유효하지 않다면 ExpectedException을 던진다")
            void it_general_throws_expected_exception() {
                List<Integer> invalidGeneralAchievements = Arrays.asList(6, 0);
                given(invalidMiddleSchoolAchievementReqDto.achievement1_2()).willReturn(invalidGeneralAchievements);
                given(invalidMiddleSchoolAchievementReqDto.achievement2_1()).willReturn(invalidGeneralAchievements);
                given(invalidMiddleSchoolAchievementReqDto.achievement2_2()).willReturn(invalidGeneralAchievements);
                given(invalidMiddleSchoolAchievementReqDto.achievement3_1()).willReturn(invalidGeneralAchievements);
                given(invalidMiddleSchoolAchievementReqDto.achievement3_2()).willReturn(invalidGeneralAchievements);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> createOneseoService.execute(reqDto, memberId));

                assertEquals("올바르지 않은 일반교과 등급이 입력되었습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }

            @Test
            @DisplayName("예체능 등급이 유효하지 않다면 ExpectedException을 던진다")
            void it_art_throws_expected_exception() {
                List<Integer> invalidArtAchievements = Arrays.asList(0, 1, 2, 6);
                given(invalidMiddleSchoolAchievementReqDto.artsPhysicalAchievement())
                        .willReturn(invalidArtAchievements);

                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> createOneseoService.execute(reqDto, memberId));

                assertEquals("올바르지 않은 예체능 등급이 입력되었습니다.", exception.getMessage());
                assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            }
        }
    }
}
