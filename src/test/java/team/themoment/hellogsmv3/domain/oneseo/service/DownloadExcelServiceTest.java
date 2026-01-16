package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.NO;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.YES;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestFactorsDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.*;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

@DisplayName("DownloadExcelService 클래스의")
public class DownloadExcelServiceTest {

    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private DownloadExcelService downloadExcelService;

    private final List<String> EXPECTED_HEADER = List.of("순번",
            "접수번호",
            "수험번호",
            "성명",
            "1지망",
            "2지망",
            "3지망",
            "생년월일",
            "성별",
            "집주소",
            "출신학교",
            "학교지역",
            "학번",
            "학력",
            "초기전형",
            "적용되는 전형",
            "일반교과점수",
            "예체능점수",
            "출석점수",
            "봉사점수",
            "1차전형총점",
            "역량평가점수",
            "심층면접점수",
            "최종점수",
            "최종학과",
            "지원자연락처",
            "보호자연락처",
            "담임연락처",
            "1차전형결과",
            "2차전형결과");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        @Nested
        @DisplayName("정상적인 데이터가 있을 때")
        class Context_with_valid_data {
            Oneseo oneseoGeneral;
            Oneseo oneseoSpecial;
            Oneseo oneseoExtra;
            Oneseo oneseoFallen;

            @BeforeEach
            void setUp() {
                oneseoGeneral = createOneseoWithAllDetails(1L, Screening.GENERAL, "A-1", YES);
                oneseoSpecial = createOneseoWithAllDetails(2L, Screening.SPECIAL, "B-2", YES);
                oneseoExtra = createOneseoWithAllDetails(3L, Screening.EXTRA_VETERANS, "C-3", YES);
                oneseoFallen = createOneseoWithAllDetails(4L, Screening.GENERAL, "A-4", NO);

                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.GENERAL))
                        .willReturn(List.of(oneseoGeneral));
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.SPECIAL))
                        .willReturn(List.of(oneseoSpecial));
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_VETERANS))
                        .willReturn(List.of(oneseoExtra));
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_ADMISSION))
                        .willReturn(List.of());
                given(oneseoRepository.findAllFailedWithAllDetails()).willReturn(List.of(oneseoFallen));
            }

            @Test
            @DisplayName("모든 전형의 시트를 생성하고 데이터를 채운다")
            void it_creates_all_sheets_with_data() throws IOException {
                try (Workbook workbook = downloadExcelService.execute()) {

                    assertNotNull(workbook);
                    assertEquals(4, workbook.getNumberOfSheets());

                    assertEquals("일반전형", workbook.getSheetAt(0).getSheetName());
                    assertEquals("특별전형", workbook.getSheetAt(1).getSheetName());
                    assertEquals("정원외 특별전형", workbook.getSheetAt(2).getSheetName());
                    assertEquals("불합격", workbook.getSheetAt(3).getSheetName());

                    assertSheetHeader(workbook.getSheetAt(0));
                    assertSheetHeader(workbook.getSheetAt(1));
                    assertSheetHeader(workbook.getSheetAt(2));
                    assertSheetHeader(workbook.getSheetAt(3));

                    assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
                    assertEquals(1, workbook.getSheetAt(1).getLastRowNum());
                    assertEquals(1, workbook.getSheetAt(2).getLastRowNum());
                    assertEquals(1, workbook.getSheetAt(3).getLastRowNum());

                    assertSheetData(workbook.getSheetAt(0), oneseoGeneral, "A-1", "일반전형");
                    assertSheetData(workbook.getSheetAt(1), oneseoSpecial, "B-2", "특별전형");
                    assertSheetData(workbook.getSheetAt(2), oneseoExtra, "C-3", "국가보훈대상자");
                    assertSheetData(workbook.getSheetAt(3), oneseoFallen, "A-4", "일반전형");
                }
            }
        }

        @Nested
        @DisplayName("빈 데이터가 있을 때")
        class Context_with_empty_data {

            @BeforeEach
            void setUp() {
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.GENERAL)).willReturn(List.of());
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.SPECIAL)).willReturn(List.of());
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_VETERANS))
                        .willReturn(List.of());
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_ADMISSION))
                        .willReturn(List.of());
                given(oneseoRepository.findAllFailedWithAllDetails()).willReturn(List.of());
            }

            @Test
            @DisplayName("헤더만 있는 시트를 생성한다")
            void it_creates_sheets_with_header_only() throws IOException {
                try (Workbook workbook = downloadExcelService.execute()) {

                    assertNotNull(workbook);
                    assertEquals(4, workbook.getNumberOfSheets());

                    for (int i = 0; i < 4; i++) {
                        Sheet sheet = workbook.getSheetAt(i);
                        assertEquals(0, sheet.getLastRowNum());
                        assertSheetHeader(sheet);
                    }
                }
            }
        }

        private void assertSheetHeader(Sheet sheet) {
            Row headerRow = sheet.getRow(0);
            assertNotNull(headerRow);

            for (int i = 0; i < EXPECTED_HEADER.size(); i++) {
                Cell cell = headerRow.getCell(i);
                assertNotNull(cell);
                assertEquals(EXPECTED_HEADER.get(i), cell.getStringCellValue());
            }
        }

        private void assertSheetData(Sheet sheet, Oneseo oneseo, String expectedSubmitCode, String expectedScreening) {
            Row dataRow = sheet.getRow(1);
            assertNotNull(dataRow);

            assertEquals("1", dataRow.getCell(0).getStringCellValue());
            assertEquals(expectedSubmitCode, dataRow.getCell(1).getStringCellValue());
            assertEquals(oneseo.getExaminationNumber(), dataRow.getCell(2).getStringCellValue());
            assertEquals(oneseo.getMember().getName(), dataRow.getCell(3).getStringCellValue());
            assertEquals("AI", dataRow.getCell(4).getStringCellValue());
            assertEquals("SW", dataRow.getCell(5).getStringCellValue());
            assertEquals("IoT", dataRow.getCell(6).getStringCellValue());
            assertEquals("20240731", dataRow.getCell(7).getStringCellValue());
            assertEquals("남자", dataRow.getCell(8).getStringCellValue());
            assertEquals("광주광역시 광산구 송정동 상무대로 312 동행관", dataRow.getCell(9).getStringCellValue());
            assertEquals("광주소프트웨어마이스터고등학교", dataRow.getCell(10).getStringCellValue());
            assertEquals("광주광역시", dataRow.getCell(11).getStringCellValue());
            assertEquals("30508", dataRow.getCell(12).getStringCellValue());
            assertEquals("졸업자", dataRow.getCell(13).getStringCellValue());
            assertEquals(expectedScreening, dataRow.getCell(14).getStringCellValue());
            assertEquals("", dataRow.getCell(15).getStringCellValue());

            assertEquals("80", dataRow.getCell(16).getStringCellValue());
            assertEquals("70", dataRow.getCell(17).getStringCellValue());
            assertEquals("60", dataRow.getCell(18).getStringCellValue());
            assertEquals("50", dataRow.getCell(19).getStringCellValue());
            assertEquals("80", dataRow.getCell(20).getStringCellValue());
            assertEquals("70", dataRow.getCell(21).getStringCellValue());
            assertEquals("60", dataRow.getCell(22).getStringCellValue());

            assertEquals("46.334", dataRow.getCell(23).getStringCellValue());

            assertEquals("IOT", dataRow.getCell(24).getStringCellValue());
            assertEquals("01012345678", dataRow.getCell(25).getStringCellValue());
            assertEquals("01087654321", dataRow.getCell(26).getStringCellValue());
            assertEquals("01012344321", dataRow.getCell(27).getStringCellValue());

            String expectedFirstResult = oneseo.getEntranceTestResult().getFirstTestPassYn() == YES ? "합격" : "불합격";
            String expectedSecondResult = oneseo.getEntranceTestResult().getSecondTestPassYn() == YES ? "합격" : "불합격";
            assertEquals(expectedFirstResult, dataRow.getCell(28).getStringCellValue());
            assertEquals(expectedSecondResult, dataRow.getCell(29).getStringCellValue());
        }

        private Oneseo createOneseoWithAllDetails(Long id, Screening screening, String submitCode, YesNo passYn) {
            String examinationNumber = String.format("0%d%02d", id, id);

            Member member = Member.builder().id(id).name("홍길동").sex(Sex.MALE).birth(LocalDate.of(2024, 7, 31))
                    .phoneNumber("01012345678").build();

            DesiredMajors desiredMajors = DesiredMajors.builder().firstDesiredMajor(Major.AI)
                    .secondDesiredMajor(Major.SW).thirdDesiredMajor(Major.IOT).build();

            EntranceTestFactorsDetail factorsDetail = EntranceTestFactorsDetail.builder().id(id)
                    .generalSubjectsScore(BigDecimal.valueOf(80)).artsPhysicalSubjectsScore(BigDecimal.valueOf(70))
                    .attendanceScore(BigDecimal.valueOf(60)).volunteerScore(BigDecimal.valueOf(50)).build();

            EntranceTestResult entranceTestResult = EntranceTestResult.builder().id(id)
                    .entranceTestFactorsDetail(factorsDetail).documentEvaluationScore(BigDecimal.valueOf(80))
                    .firstTestPassYn(passYn).secondTestPassYn(passYn).competencyEvaluationScore(BigDecimal.valueOf(70))
                    .interviewScore(BigDecimal.valueOf(60)).build();

            OneseoPrivacyDetail privacyDetail = OneseoPrivacyDetail.builder().id(id).schoolName("광주소프트웨어마이스터고등학교")
                    .address("광주광역시 광산구 송정동 상무대로 312").schoolAddress("광주광역시 광산구 송정동 상무대로 312").detailAddress("동행관")
                    .guardianPhoneNumber("01087654321").schoolTeacherPhoneNumber("01012344321")
                    .graduationType(GraduationType.GRADUATE).studentNumber("30508").build();

            return Oneseo.builder().id(id).member(member).oneseoSubmitCode(submitCode)
                    .examinationNumber(examinationNumber).desiredMajors(desiredMajors).wantedScreening(screening)
                    .decidedMajor(Major.IOT).entranceTestResult(entranceTestResult).oneseoPrivacyDetail(privacyDetail)
                    .build();
        }
    }

    @Nested
    @DisplayName("점수 계산 로직은")
    class Describe_score_calculation {

        @Nested
        @DisplayName("유효한 점수가 있을 때")
        class Context_with_valid_scores {

            @Test
            @DisplayName("최종 점수를 올바르게 계산한다")
            void it_calculates_final_score_correctly() throws IOException {
                Oneseo oneseo = createOneseoWithScores(BigDecimal.valueOf(90),
                        BigDecimal.valueOf(80),
                        BigDecimal.valueOf(75));

                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.GENERAL)).willReturn(List.of(oneseo));
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.SPECIAL)).willReturn(List.of());
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_VETERANS))
                        .willReturn(List.of());
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_ADMISSION))
                        .willReturn(List.of());
                given(oneseoRepository.findAllFailedWithAllDetails()).willReturn(List.of());

                try (Workbook workbook = downloadExcelService.execute()) {

                    Sheet sheet = workbook.getSheetAt(0);
                    Row dataRow = sheet.getRow(1);

                    assertEquals("54.000", dataRow.getCell(23).getStringCellValue());
                }
            }
        }

        @Nested
        @DisplayName("null 점수가 있을 때")
        class Context_with_null_scores {

            @Test
            @DisplayName("빈 문자열을 반환한다")
            void it_returns_empty_string_for_null_scores() throws IOException {
                Oneseo oneseo = createOneseoWithNullScores();

                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.GENERAL)).willReturn(List.of(oneseo));
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.SPECIAL)).willReturn(List.of());
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_VETERANS))
                        .willReturn(List.of());
                given(oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_ADMISSION))
                        .willReturn(List.of());
                given(oneseoRepository.findAllFailedWithAllDetails()).willReturn(List.of());

                try (Workbook workbook = downloadExcelService.execute()) {

                    Sheet sheet = workbook.getSheetAt(0);
                    Row dataRow = sheet.getRow(1);

                    assertEquals("", dataRow.getCell(15).getStringCellValue());
                    assertEquals("", dataRow.getCell(19).getStringCellValue());
                    assertEquals("", dataRow.getCell(20).getStringCellValue());
                    assertEquals("", dataRow.getCell(21).getStringCellValue());
                    assertEquals("", dataRow.getCell(22).getStringCellValue());
                }
            }
        }

        private Oneseo createOneseoWithScores(BigDecimal documentScore,
                BigDecimal competencyScore,
                BigDecimal interviewScore) {
            Member member = Member.builder().id(1L).name("테스트").sex(Sex.MALE).birth(LocalDate.of(2024, 7, 31))
                    .phoneNumber("01012345678").build();

            DesiredMajors desiredMajors = DesiredMajors.builder().firstDesiredMajor(Major.AI)
                    .secondDesiredMajor(Major.SW).thirdDesiredMajor(Major.IOT).build();

            EntranceTestFactorsDetail factorsDetail = EntranceTestFactorsDetail.builder().id(1L)
                    .generalSubjectsScore(BigDecimal.valueOf(80)).artsPhysicalSubjectsScore(BigDecimal.valueOf(70))
                    .attendanceScore(BigDecimal.valueOf(60)).volunteerScore(BigDecimal.valueOf(50)).build();

            EntranceTestResult entranceTestResult = EntranceTestResult.builder().id(1L)
                    .entranceTestFactorsDetail(factorsDetail).documentEvaluationScore(documentScore)
                    .firstTestPassYn(YES).secondTestPassYn(YES).competencyEvaluationScore(competencyScore)
                    .interviewScore(interviewScore).build();

            OneseoPrivacyDetail privacyDetail = OneseoPrivacyDetail.builder().id(1L).schoolName("테스트고등학교")
                    .address("테스트주소").detailAddress("상세주소").guardianPhoneNumber("01087654321")
                    .schoolTeacherPhoneNumber("01012344321").graduationType(GraduationType.GRADUATE)
                    .studentNumber("30508").build();

            return Oneseo.builder().id(1L).member(member).oneseoSubmitCode("A-1").examinationNumber("0101")
                    .desiredMajors(desiredMajors).wantedScreening(Screening.GENERAL).decidedMajor(Major.IOT)
                    .entranceTestResult(entranceTestResult).oneseoPrivacyDetail(privacyDetail).build();
        }

        private Oneseo createOneseoWithNullScores() {
            Member member = Member.builder().id(1L).name("테스트").sex(Sex.MALE).birth(LocalDate.of(2024, 7, 31))
                    .phoneNumber("01012345678").build();

            DesiredMajors desiredMajors = DesiredMajors.builder().firstDesiredMajor(Major.AI)
                    .secondDesiredMajor(Major.SW).thirdDesiredMajor(Major.IOT).build();

            EntranceTestFactorsDetail factorsDetail = EntranceTestFactorsDetail.builder().id(1L)
                    .generalSubjectsScore(null).artsPhysicalSubjectsScore(null).attendanceScore(null)
                    .volunteerScore(null).build();

            EntranceTestResult entranceTestResult = EntranceTestResult.builder().id(1L)
                    .entranceTestFactorsDetail(factorsDetail).documentEvaluationScore(null).firstTestPassYn(YES)
                    .secondTestPassYn(null).competencyEvaluationScore(null).interviewScore(null).build();

            OneseoPrivacyDetail privacyDetail = OneseoPrivacyDetail.builder().id(1L).schoolName("테스트고등학교")
                    .address("테스트주소").detailAddress("상세주소").guardianPhoneNumber("01087654321")
                    .schoolTeacherPhoneNumber("01012344321").graduationType(GraduationType.GRADUATE).build();

            return Oneseo.builder().id(1L).member(member).oneseoSubmitCode("A-1").examinationNumber("0101")
                    .desiredMajors(desiredMajors).wantedScreening(Screening.GENERAL).decidedMajor(Major.IOT)
                    .entranceTestResult(entranceTestResult).oneseoPrivacyDetail(privacyDetail).build();
        }
    }
}
