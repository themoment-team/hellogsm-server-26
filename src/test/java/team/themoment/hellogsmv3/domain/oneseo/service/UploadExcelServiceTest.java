package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.repository.EntranceTestResultRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("UploadExcelService 클래스의")
class UploadExcelServiceTest {

    @Mock
    private EntranceTestResultRepository entranceTestResultRepository;
    @Mock
    private OneseoRepository oneseoRepository;

    @InjectMocks
    private UploadExcelService uploadExcelService;

    private AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        @Nested
        @DisplayName("혼합 타입(문자/숫자/수식) 유효 데이터가 있을 때")
        class Context_with_valid_mixed_cells {
            @Test
            @DisplayName("점수를 반올림하여 저장한다")
            void it_saves_with_rounded_scores() throws Exception {
                // given
                String exam1 = "1001";
                String exam2 = "1002";

                EntranceTestResult r1 = mockEntranceResult(new BigDecimal("10"), new BigDecimal("11"));
                EntranceTestResult r2 = mockEntranceResult(new BigDecimal("20"), new BigDecimal("21"));
                Map<String, EntranceTestResult> map = new HashMap<>();
                map.put(exam1, r1);
                map.put(exam2, r2);
                when(oneseoRepository.findEntranceTestResultByExaminationNumbersIn(any())).thenReturn(map);
                when(entranceTestResultRepository.saveAll(any())).thenAnswer(inv -> {
                    Iterable<EntranceTestResult> it = inv.getArgument(0);
                    List<EntranceTestResult> list = new ArrayList<>();
                    it.forEach(list::add);
                    return list;
                });

                MultipartFile file = buildWorkbook(wb -> {
                    Sheet sheet = wb.createSheet();
                    Row header = sheet.createRow(0);
                    header.createCell(0).setCellValue("수험번호");
                    header.createCell(3).setCellValue("역검점수");
                    header.createCell(4).setCellValue("면접점수");
                    Row row1 = sheet.createRow(1);
                    row1.createCell(0).setCellValue(exam1);
                    row1.createCell(3).setCellValue(70.2);
                    row1.createCell(4).setCellValue(60);
                    Row row2 = sheet.createRow(2);
                    row2.createCell(0).setCellValue(Double.parseDouble(exam2));
                    Cell cFormula = row2.createCell(3);
                    cFormula.setCellFormula("40+20.255");
                    row2.createCell(4).setCellValue("65.2399");
                });

                // when
                uploadExcelService.execute(file);

                // then
                ArgumentCaptor<Collection<EntranceTestResult>> captor = ArgumentCaptor.forClass(Collection.class);
                verify(entranceTestResultRepository, times(1)).saveAll(captor.capture());
                List<EntranceTestResult> saved = new ArrayList<>(captor.getValue());
                assertEquals(2, saved.size());
                assertEquals(new BigDecimal("70.20"), r1.getCompetencyEvaluationScore());
                assertEquals(new BigDecimal("60.00"), r1.getInterviewScore());
                assertEquals(new BigDecimal("60.26"), r2.getCompetencyEvaluationScore());
                assertEquals(new BigDecimal("65.24"), r2.getInterviewScore());
            }
        }

        @Nested
        @DisplayName("중복 수험번호가 포함되면")
        class Context_with_duplicate_examination_numbers {
            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_duplicate_exception() throws Exception {
                MultipartFile file = buildWorkbook(wb -> {
                    Sheet sheet = wb.createSheet();
                    Row header = sheet.createRow(0);
                    header.createCell(0).setCellValue("수험번호");
                    header.createCell(3).setCellValue("역검점수");
                    header.createCell(4).setCellValue("면접점수");
                    Row r1 = sheet.createRow(1);
                    r1.createCell(0).setCellValue("1001");
                    r1.createCell(3).setCellValue("10");
                    r1.createCell(4).setCellValue("20");
                    Row r2 = sheet.createRow(2);
                    r2.createCell(0).setCellValue("1001");
                    r2.createCell(3).setCellValue("30");
                    r2.createCell(4).setCellValue("40");
                });
                ExpectedException ex = assertThrows(ExpectedException.class, () -> uploadExcelService.execute(file));
                assertTrue(ex.getMessage().contains("중복"));
                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            }
        }

        @Nested
        @DisplayName("범위를 벗어난 점수가 포함되면")
        class Context_with_out_of_range_score {
            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_out_of_range_exception() throws Exception {
                mockFindEntranceResults("1001");
                MultipartFile file = buildWorkbook(wb -> {
                    Sheet sheet = wb.createSheet();
                    Row header = sheet.createRow(0);
                    header.createCell(0).setCellValue("수험번호");
                    header.createCell(3).setCellValue("역검점수");
                    header.createCell(4).setCellValue("면접점수");
                    Row r1 = sheet.createRow(1);
                    r1.createCell(0).setCellValue("1001");
                    r1.createCell(3).setCellValue(150);
                    r1.createCell(4).setCellValue(10);
                });
                ExpectedException ex = assertThrows(ExpectedException.class, () -> uploadExcelService.execute(file));
                assertTrue(ex.getMessage().contains("0 이상 100 이하"));
                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            }
        }

        @Nested
        @DisplayName("숫자 형식이 잘못된 점수가 포함되면")
        class Context_with_invalid_number_format {
            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_number_format_exception() throws Exception {
                mockFindEntranceResults("1001");
                MultipartFile file = buildWorkbook(wb -> {
                    Sheet sheet = wb.createSheet();
                    Row header = sheet.createRow(0);
                    header.createCell(0).setCellValue("수험번호");
                    header.createCell(3).setCellValue("역검점수");
                    header.createCell(4).setCellValue("면접점수");
                    Row r1 = sheet.createRow(1);
                    r1.createCell(0).setCellValue("1001");
                    r1.createCell(3).setCellValue("7O.2");
                    r1.createCell(4).setCellValue("60");
                });
                ExpectedException ex = assertThrows(ExpectedException.class, () -> uploadExcelService.execute(file));
                assertTrue(ex.getMessage().contains("점수 형식"));
                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            }
        }

        @Nested
        @DisplayName("필수 셀이 비어있는 행이 포함되면")
        class Context_with_blank_required_cell {
            @Test
            @DisplayName("ExpectedException을 던진다")
            void it_throws_blank_cell_exception() throws Exception {
                MultipartFile file = buildWorkbook(wb -> {
                    Sheet sheet = wb.createSheet();
                    Row header = sheet.createRow(0);
                    header.createCell(0).setCellValue("수험번호");
                    header.createCell(3).setCellValue("역검점수");
                    header.createCell(4).setCellValue("면접점수");
                    Row r1 = sheet.createRow(1);
                    r1.createCell(0).setBlank();
                    r1.createCell(3).setCellValue(10);
                    r1.createCell(4).setCellValue(20);
                });
                ExpectedException ex = assertThrows(ExpectedException.class, () -> uploadExcelService.execute(file));
                assertTrue(ex.getMessage().contains("필수 셀이 비어있습니다"));
                assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            }
        }

        @Nested
        @DisplayName("DB에 존재하지 않는 수험번호만 포함되면")
        class Context_with_not_found_examination_number {
        @Test
      @DisplayName("ExpectedException을 던진다")
      void it_throws_not_found_exception() throws Exception {
        when(oneseoRepository.findEntranceTestResultByExaminationNumbersIn(any()))
            .thenReturn(Collections.emptyMap());
        MultipartFile file =
            buildWorkbook(
                wb -> {
                  Sheet sheet = wb.createSheet();
                  Row header = sheet.createRow(0);
                  header.createCell(0).setCellValue("수험번호");
                  header.createCell(3).setCellValue("역검점수");
                  header.createCell(4).setCellValue("면접점수");
                  Row r1 = sheet.createRow(1);
                  r1.createCell(0).setCellValue("9999");
                  r1.createCell(3).setCellValue(10);
                  r1.createCell(4).setCellValue(20);
                });
        ExpectedException ex =
            assertThrows(ExpectedException.class, () -> uploadExcelService.execute(file));
        assertTrue(ex.getMessage().contains("존재하지 않습니다"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
      }
        }
    }

    private MultipartFile buildWorkbook(WorkbookConsumer consumer) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            consumer.accept(wb);
            wb.write(bos);
            return new MockMultipartFile("file",
                    "test.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    bos.toByteArray());
        }
    }

    private EntranceTestResult mockEntranceResult(BigDecimal competency, BigDecimal interview) {
        EntranceTestResult result = mock(EntranceTestResult.class);
        final BigDecimal[] compHolder = {competency};
        final BigDecimal[] interHolder = {interview};
        when(result.getCompetencyEvaluationScore()).thenAnswer(a -> compHolder[0]);
        when(result.getInterviewScore()).thenAnswer(a -> interHolder[0]);
        doAnswer(a -> {
            compHolder[0] = a.getArgument(0);
            return null;
        }).when(result).modifyCompetencyEvaluationScore(any(BigDecimal.class));
        doAnswer(a -> {
            interHolder[0] = a.getArgument(0);
            return null;
        }).when(result).modifyInterviewScore(any(BigDecimal.class));
        return result;
    }

    private void mockFindEntranceResults(String exam) {
        EntranceTestResult r = mockEntranceResult(BigDecimal.ZERO, BigDecimal.ZERO);
        Map<String, EntranceTestResult> map = new HashMap<>();
        map.put(exam, r);
        when(oneseoRepository.findEntranceTestResultByExaminationNumbersIn(any())).thenReturn(map);
        when(entranceTestResultRepository.saveAll(any())).thenAnswer(inv -> {
            Iterable<EntranceTestResult> it = inv.getArgument(0);
            List<EntranceTestResult> list = new ArrayList<>();
            it.forEach(list::add);
            return list;
        });
    }

    @FunctionalInterface
    private interface WorkbookConsumer {
        void accept(XSSFWorkbook wb);
    }
}
