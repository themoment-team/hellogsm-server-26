package team.themoment.hellogsmv3.domain.oneseo.service;

import static team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening.EXTRA_VETERANS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.OneseoPrivacyDetail;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class DownloadExcelService {

    private final OneseoRepository oneseoRepository;

    private static final int BATCH_SIZE = 1000; // 한 번에 처리할 원서 데이터의 배치 크기
    private static final int ROW_ACCESS_WINDOW = 100; // SXSSFWorkbook의 메모리 사용 최적화를 위한 행 접근 윈도우 크기

    private static final List<String> HEADER_NAMES = List.of("순번",
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

    private static final Map<Screening, String> SCREENING_DISPLAY_MAP = Map.of(Screening.GENERAL,
            "일반전형",
            Screening.SPECIAL,
            "특별전형",
            EXTRA_VETERANS,
            "국가보훈대상자",
            Screening.EXTRA_ADMISSION,
            "특례입학대상자");

    private static final Map<YesNo, String> PASS_YN_DISPLAY_MAP = Map.of(YesNo.YES, "합격", YesNo.NO, "불합격");

    @Transactional(readOnly = true)
    public Workbook execute() {
        SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW);

        try {
            List<Sheet> sheets = createSheets(workbook);

            List<List<String>> generalData = getOneseoDataOptimized(Screening.GENERAL);
            populateSheet(sheets.get(0), generalData);

            List<List<String>> specialData = getOneseoDataOptimized(Screening.SPECIAL);
            populateSheet(sheets.get(1), specialData);

            List<List<String>> extraData = getCombinedExtraScreeningDataOptimized();
            populateSheet(sheets.get(2), extraData);

            List<List<String>> fallenData = getFallenDataOptimized();
            populateSheet(sheets.get(3), fallenData);
            return workbook;
        } catch (Exception e) {
            try {
                workbook.close();
            } catch (Exception closeEx) {
                throw new ExpectedException("Excel 파일 닫기 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            throw new ExpectedException("Excel 파일 생성 중 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private List<Sheet> createSheets(Workbook workbook) {
        List<String> sheetNames = List.of("일반전형", "특별전형", "정원외 특별전형", "불합격");
        List<Sheet> sheets = new ArrayList<>();

        for (String sheetName : sheetNames) {
            Sheet sheet = workbook.createSheet(sheetName);
            createHeaderRow(sheet);
            sheets.add(sheet);
        }

        return sheets;
    }

    private void createHeaderRow(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < HEADER_NAMES.size(); i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADER_NAMES.get(i));
        }
    }

    private void populateSheet(Sheet sheet, List<List<String>> data) {
        int rowIndex = 1;

        for (List<String> rowData : data) {
            Row row = sheet.createRow(rowIndex++);
            for (int i = 0; i < rowData.size(); i++) {
                Cell cell = row.createCell(i);
                cell.setCellValue(rowData.get(i));
            }
        }
    }

    private List<List<String>> getOneseoDataOptimized(Screening screening) {
        List<Oneseo> oneseoList = oneseoRepository.findAllByScreeningWithAllDetails(screening);
        return processOneseoDataOptimized(oneseoList);
    }

    private List<List<String>> getCombinedExtraScreeningDataOptimized() {
        List<Oneseo> extraVeterans = oneseoRepository.findAllByScreeningWithAllDetails(EXTRA_VETERANS);
        List<Oneseo> extraAdmission = oneseoRepository.findAllByScreeningWithAllDetails(Screening.EXTRA_ADMISSION);

        List<Oneseo> combinedList = Stream.concat(extraVeterans.stream(), extraAdmission.stream())
                .collect(Collectors.toList());

        return processOneseoDataOptimized(combinedList);
    }

    private List<List<String>> getFallenDataOptimized() {
        List<Oneseo> fallenOneseoList = oneseoRepository.findAllFailedWithAllDetails();
        return processOneseoDataOptimized(fallenOneseoList);
    }

    private List<List<String>> processOneseoDataOptimized(List<Oneseo> oneseoList) {
        if (oneseoList.isEmpty()) {
            return new ArrayList<>();
        }
        List<List<String>> result = new ArrayList<>();

        for (int i = 0; i < oneseoList.size(); i += BATCH_SIZE) {
            int endIndex = Math.min(i + BATCH_SIZE, oneseoList.size());
            List<Oneseo> batch = oneseoList.subList(i, endIndex);

            List<List<String>> batchResult = processBatch(batch, i + 1);
            result.addAll(batchResult);
        }

        return result;
    }

    private List<List<String>> processBatch(List<Oneseo> batchOneseoList, int startIndex) {
        List<List<String>> batchResult = new ArrayList<>();

        for (int i = 0; i < batchOneseoList.size(); i++) {
            Oneseo oneseo = batchOneseoList.get(i);
            EntranceTestResult entranceTestResult = oneseo.getEntranceTestResult();
            OneseoPrivacyDetail privacyDetail = oneseo.getOneseoPrivacyDetail();

            List<String> rowData = createRowData(oneseo, entranceTestResult, privacyDetail, startIndex + i);
            batchResult.add(rowData);
        }

        return batchResult;
    }

    private List<String> createRowData(Oneseo oneseo,
            EntranceTestResult entranceTestResult,
            OneseoPrivacyDetail oneseoPrivacyDetail,
            int index) {

        BigDecimal finalScore = calculateFinalScore(entranceTestResult);

        String firstDesiredMajor = convertMajorDisplayName(oneseo.getDesiredMajors().getFirstDesiredMajor());
        String secondDesiredMajor = convertMajorDisplayName(oneseo.getDesiredMajors().getSecondDesiredMajor());
        String thirdDesiredMajor = convertMajorDisplayName(oneseo.getDesiredMajors().getThirdDesiredMajor());

        return List.of(String.valueOf(index),
                safeToString(oneseo.getOneseoSubmitCode()),
                safeToString(oneseo.getExaminationNumber()),
                safeToString(oneseo.getMember().getName()),
                firstDesiredMajor,
                secondDesiredMajor,
                thirdDesiredMajor,
                formatBirth(oneseo.getMember().getBirth()),
                convertSex(oneseo.getMember().getSex()),
                buildAddress(oneseoPrivacyDetail),
                safeToString(oneseoPrivacyDetail.getSchoolName()),
                getMetropolitanName(oneseoPrivacyDetail.getSchoolAddress()),
                formatStudentNumber(oneseoPrivacyDetail.getStudentNumber()),
                convertGraduationType(oneseoPrivacyDetail.getGraduationType()),
                convertScreening(oneseo.getWantedScreening()),
                convertScreening(oneseo.getAppliedScreening()),
                getScoreString(entranceTestResult.getEntranceTestFactorsDetail().getGeneralSubjectsScore()),
                getScoreString(entranceTestResult.getEntranceTestFactorsDetail().getArtsPhysicalSubjectsScore()),
                getScoreString(entranceTestResult.getEntranceTestFactorsDetail().getAttendanceScore()),
                getScoreString(entranceTestResult.getEntranceTestFactorsDetail().getVolunteerScore()),
                getScoreString(entranceTestResult.getDocumentEvaluationScore()),
                getScoreString(entranceTestResult.getCompetencyEvaluationScore()),
                getScoreString(entranceTestResult.getInterviewScore()),
                getScoreString(finalScore),
                safeToString(oneseo.getDecidedMajor()),
                safeToString(oneseo.getMember().getPhoneNumber()),
                safeToString(oneseoPrivacyDetail.getGuardianPhoneNumber()),
                safeToString(oneseoPrivacyDetail.getSchoolTeacherPhoneNumber()),
                convertTestPassYn(entranceTestResult.getFirstTestPassYn()),
                convertTestPassYn(entranceTestResult.getSecondTestPassYn()));
    }

    private String safeToString(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private String convertMajorDisplayName(Major major) {
        if (major == null)
            return "";
        return major == Major.IOT ? "IoT" : major.toString();
    }

    private String formatBirth(LocalDate birth) {
        return birth.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private String getMetropolitanName(String address) {
        if (address == null)
            return "검정고시";
        String[] parts = address.split(" ");
        return parts[0];
    }

    private String formatStudentNumber(String studentNumber) {
        if (studentNumber == null)
            return "검정고시";
        return studentNumber;
    }

    private String convertSex(team.themoment.hellogsmv3.domain.member.entity.type.Sex sex) {
        if (sex == null)
            return "";
        return switch (sex) {
            case MALE -> "남자";
            case FEMALE -> "여자";
        };
    }

    private String buildAddress(OneseoPrivacyDetail privacyDetail) {
        if (privacyDetail == null)
            return "";
        String address = privacyDetail.getAddress();
        String detailAddress = privacyDetail.getDetailAddress();
        return (address != null ? address : "") + (detailAddress != null ? " " + detailAddress : "");
    }

    private String convertGraduationType(
            team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType graduationType) {
        if (graduationType == null)
            return "";
        return switch (graduationType) {
            case CANDIDATE -> "졸업예정자";
            case GRADUATE -> "졸업자";
            case GED -> "검정고시";
        };
    }

    private String convertScreening(Screening screening) {
        if (screening == null)
            return "";
        return SCREENING_DISPLAY_MAP.getOrDefault(screening, "");
    }

    private String convertTestPassYn(YesNo yn) {
        if (yn == null)
            return "";
        return PASS_YN_DISPLAY_MAP.getOrDefault(yn, "");
    }

    private String getScoreString(BigDecimal score) {
        return score != null ? score.toString() : "";
    }

    private BigDecimal calculateFinalScore(EntranceTestResult entranceTestResult) {
        if (entranceTestResult == null)
            return null;

        BigDecimal competencyEvaluationScore = entranceTestResult.getCompetencyEvaluationScore();
        BigDecimal interviewScore = entranceTestResult.getInterviewScore();
        BigDecimal documentEvaluationScore = entranceTestResult.getDocumentEvaluationScore();

        if (entranceTestResult.getSecondTestPassYn() == null || competencyEvaluationScore == null
                || interviewScore == null || documentEvaluationScore == null) {
            return null;
        }

        BigDecimal adjustedDocumentScore = documentEvaluationScore
                .divide(BigDecimal.valueOf(3), 3, RoundingMode.HALF_UP);

        return adjustedDocumentScore.multiply(BigDecimal.valueOf(0.5))
                .add(competencyEvaluationScore.multiply(BigDecimal.valueOf(0.3)))
                .add(interviewScore.multiply(BigDecimal.valueOf(0.2))).setScale(3, RoundingMode.HALF_UP);
    }
}
