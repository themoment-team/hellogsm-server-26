package team.themoment.hellogsmv3.domain.oneseo.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.SecondTestResultDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class UploadExcelService {

    private final EntranceTestResultRepository entranceTestResultRepository;
    private final OneseoRepository oneseoRepository;

    private static final DataFormatter DATA_FORMATTER = new DataFormatter();

    @Getter
    private enum CellIndex {
        EXAMINATION_NUMBER(0), COMPETENCY_EVALUATION_SCORE(3), INTERVIEW_SCORE(4);
        private final int index;

        CellIndex(int index) {
            this.index = index;
        }
    }

    public void execute(MultipartFile file) {
        try (Workbook workbook = createWorkbookFromFile(file)) {
            Sheet sheet = workbook.getNumberOfSheets() == 0 ? null : workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ExpectedException("엑셀 첫 번째 시트를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST);
            }
            Map<String, SecondTestResultDto> excelResults = getExcelTestResult(sheet, workbook);

            Set<String> examinationNumberToProcess = excelResults.keySet();
            Map<String, EntranceTestResult> queryResult = oneseoRepository
                    .findEntranceTestResultByExaminationNumbersIn(examinationNumberToProcess.stream().toList());

            validateNotFoundExaminationNumbers(examinationNumberToProcess, queryResult.keySet());
            saveAllEntranceTestResults(queryResult, excelResults);
        } catch (IOException e) {
            throw new ExpectedException("엑셀 파일을 읽는 데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, SecondTestResultDto> getExcelTestResult(Sheet sheet, Workbook wb) {
        Map<String, SecondTestResultDto> excelResults = new HashMap<>();
        Set<String> seenExaminationNumbers = new HashSet<>();
        Set<String> duplicateExaminationNumbers = new HashSet<>();
        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null)
                continue;

            String examinationNumber = readTextCell(row, CellIndex.EXAMINATION_NUMBER, i, evaluator);
            BigDecimal competencyEvaluationScore = readScoreCell(row,
                    CellIndex.COMPETENCY_EVALUATION_SCORE,
                    i,
                    evaluator);
            BigDecimal interviewScore = readScoreCell(row, CellIndex.INTERVIEW_SCORE, i, evaluator);

            if (!seenExaminationNumbers.add(examinationNumber)) {
                duplicateExaminationNumbers.add(examinationNumber);
            }
            excelResults.put(examinationNumber, new SecondTestResultDto(competencyEvaluationScore, interviewScore));
        }
        if (!duplicateExaminationNumbers.isEmpty()) {
            throw new ExpectedException("다음 수험번호가 중복되었습니다: " + String.join(", ", duplicateExaminationNumbers),
                    HttpStatus.BAD_REQUEST);
        }
        return excelResults;
    }

    private void validateNotFoundExaminationNumbers(Set<String> expected, Set<String> found) {
        Set<String> notFoundExaminationNumbers = expected.stream()
                .filter(examinationNumber -> !found.contains(examinationNumber)).collect(Collectors.toSet());
        if (!notFoundExaminationNumbers.isEmpty()) {
            throw new ExpectedException(
                    "다음 수험번호에 대한 응시결과(원서)가 존재하지 않습니다: " + String.join(", ", notFoundExaminationNumbers),
                    HttpStatus.NOT_FOUND);
        }
    }

    private void saveAllEntranceTestResults(Map<String, EntranceTestResult> resultToModify,
            Map<String, SecondTestResultDto> excelResults) {
        resultToModify.forEach((examinationNumber, entranceTestResult) -> {
            SecondTestResultDto dto = excelResults.get(examinationNumber);
            entranceTestResult.modifyCompetencyEvaluationScore(dto.competencyEvaluationScore());
            entranceTestResult.modifyInterviewScore(dto.interviewScore());
        });
        entranceTestResultRepository.saveAll(resultToModify.values());
    }

    private Workbook createWorkbookFromFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ExpectedException("엑셀 파일이 비어있습니다.", HttpStatus.BAD_REQUEST);
        }
        return WorkbookFactory.create(file.getInputStream());
    }

    private BigDecimal readScoreCell(Row row, CellIndex cellIndex, int rowIdx, FormulaEvaluator evaluator) {
        String raw = readTextCell(row, cellIndex, rowIdx, evaluator);
        BigDecimal score;
        try {
            score = new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new ExpectedException(positionMsg(rowIdx, cellIndex, "점수 형식이 올바르지 않습니다."), HttpStatus.BAD_REQUEST);
        }
        score = score.setScale(2, RoundingMode.HALF_UP);
        if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(new BigDecimal("100")) > 0) {
            throw new ExpectedException(positionMsg(rowIdx, cellIndex, "점수는 0 이상 100 이하의 값이어야 합니다."),
                    HttpStatus.BAD_REQUEST);
        }
        return score;
    }

    private String readTextCell(Row row, CellIndex cellIndex, int rowIdx, FormulaEvaluator evaluator) {
        Cell cell = row.getCell(cellIndex.getIndex());
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            throw new ExpectedException(positionMsg(rowIdx, cellIndex, "필수 셀이 비어있습니다."), HttpStatus.BAD_REQUEST);
        }
        String value;
        if (cell.getCellType() == CellType.FORMULA) {
            value = DATA_FORMATTER.formatCellValue(cell, evaluator);
        } else if (cell.getCellType() == CellType.NUMERIC) {
            value = DATA_FORMATTER.formatCellValue(cell);
        } else {
            value = cell.getCellType() == CellType.STRING
                    ? cell.getStringCellValue()
                    : DATA_FORMATTER.formatCellValue(cell);
        }
        value = value.trim();
        if (value.isEmpty()) {
            throw new ExpectedException(positionMsg(rowIdx, cellIndex, "필수 셀이 비어있습니다."), HttpStatus.BAD_REQUEST);
        }
        return value;
    }

    private String positionMsg(int rowIdx, CellIndex cellIndex, String msg) {
        int displayRow = rowIdx + 1;
        char col = (char) ('A' + cellIndex.getIndex());
        return msg + " (위치: " + col + displayRow + ")";
    }
}
