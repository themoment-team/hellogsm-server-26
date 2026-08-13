package team.themoment.hellogsmv3.domain.oneseo.service.extraction;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.ExtractedAchievementResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.global.thirdParty.ocr.TesseractTextExtractor;
import team.themoment.hellogsmv3.global.thirdParty.pdf.PdfBoxTextExtractor;

/**
 * PDF 생성부터 파싱까지 전체 경로를 한 번에 통과시키는 진단 테스트입니다.
 *
 * <p>
 * 기본적으로는 생활기록부 형태의 PDF를 직접 만들어 검증합니다. 실제 생활기록부로 확인하려면 환경변수로 파일 경로를 넘기세요.
 *
 * <pre>
 * RECORD_PDF=/path/to/생활기록부.pdf ./gradlew test --tests "*RecordExtractionDiagnosticTest" -i
 * </pre>
 *
 * <p>
 * 이때는 추출 원문과 파싱 결과를 콘솔에 출력하기만 하므로, 정규식을 어떻게 조정해야 하는지 바로 확인할 수 있습니다.
 */
@DisplayName("생활기록부 추출 전체 경로 진단")
class RecordExtractionDiagnosticTest {

    /** 실제 생활기록부 PDF 경로. 지정하면 합성 PDF 대신 이 파일을 사용합니다. */
    private static final String REAL_PDF_ENV = "RECORD_PDF";

    /** 실제 파일이라도 추출 원문 전체를 출력하고 싶을 때 사용합니다. 개인정보가 그대로 노출됩니다. */
    private static final String FULL_TEXT_ENV = "RECORD_PDF_FULL_TEXT";

    private static final String FONT_PATH = "C:/Windows/Fonts/malgun.ttf";

    /** 표 형태를 흉내내기 위한 열 x좌표 */
    private static final float[] COLUMN_X = {50, 90, 160, 250, 400, 480};

    private static final String[][] ROWS = {{"[1학년]"}, {"학기", "교과", "과목", "원점수/과목평균(표준편차)", "성취도(수강자수)", "비고"},
            {"2", "국어", "국어", "85/70.5(12.3)", "A(250)"}, {"2", "사회(역사포함)", "사회", "78/65.2(14.1)", "B(250)"},
            {"2", "수학", "수학", "91/68.0(15.0)", "A(250)"}, {"2", "기술·가정", "기술·가정", "82/70.0(12.0)", "B(250)"},
            {"2", "정보", "정보", "88/70.0(12.0)", "A(250)"}, {"2", "체육", "체육", "A"}, {"[2학년]"},
            {"학기", "교과", "과목", "원점수/과목평균(표준편차)", "성취도(수강자수)", "비고"}, {"1", "국어", "국어", "88/71.2(11.9)", "A(248)"},
            {"1", "사회(역사포함)", "사회", "70/66.0(13.0)", "C(248)"}, {"1", "수학", "수학", "60/64.0(16.0)", "D(248)"},
            {"2", "국어", "국어", "80/70.0(12.0)", "B(248)"}, {"2", "사회(역사포함)", "사회", "75/66.5(13.5)", "B(248)"},
            {"2", "수학", "수학", "95/69.0(14.5)", "A(248)"}, {"[3학년]"},
            {"학기", "교과", "과목", "원점수/과목평균(표준편차)", "성취도(수강자수)", "비고"}, {"1", "국어", "국어", "92/72.0(11.0)", "A(245)"},
            {"1", "사회(역사포함)", "사회", "84/67.0(12.5)", "B(245)"}, {"1", "수학", "수학", "77/65.0(15.5)", "C(245)"}, {"출결상황"},
            {"1", "190", "2 0 0 1 0 0 0 0 0 0 0 0"}, {"창의적 체험활동상황"}, {"1", "봉사활동", "7"}};

    private byte[] createSyntheticRecordPdf() throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            PDType0Font font = PDType0Font.load(document, new File(FONT_PATH));

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setFont(font, 9);
                float y = 800;
                for (String[] row : ROWS) {
                    for (int col = 0; col < row.length; col++) {
                        content.beginText();
                        content.newLineAtOffset(COLUMN_X[Math.min(col, COLUMN_X.length - 1)], y);
                        content.showText(row[col]);
                        content.endText();
                    }
                    y -= 14;
                }
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * 성적 표처럼 보이는 줄만 고르는 느슨한 필터입니다.
     *
     * <p>
     * 생활기록부에는 이름, 학교, 세부능력특기사항 등 진단에 불필요한 개인정보가 함께 담겨 있으므로, 실제 파일을 넣을 때는 성적·출결·봉사
     * 관련 줄만 출력합니다.
     */
    private static final Pattern DIAGNOSTIC_LINE = Pattern
            .compile("^[123]?\\s*학년|^\\[?[123]\\s*학년|^[12]\\s+\\S+\\s+\\S+.*[A-E]|^[123](\\s+\\d+){5,}|봉사활동\\s+\\d+");

    /**
     * 서비스와 동일한 순서로 추출을 시도합니다. 텍스트 레이어가 없으면 OCR로 넘어갑니다.
     *
     * <p>
     * OCR 경로는 {@code TESSDATA_PATH} 환경변수로 학습 데이터 위치를 지정해야 동작합니다.
     */
    private ExtractedTextDto extractWithFallback(byte[] pdfBytes) throws IOException {
        ExtractedTextDto viaTextLayer = new PdfBoxTextExtractor().extract(pdfBytes, "application/pdf");
        if (viaTextLayer.rawText().strip().length() >= 200) {
            return viaTextLayer;
        }

        String tessdataPath = System.getenv("TESSDATA_PATH");
        Assumptions.assumeTrue(tessdataPath != null && !tessdataPath.isBlank(),
                "텍스트 레이어가 없는 PDF입니다. OCR로 확인하려면 TESSDATA_PATH를 지정하세요.");

        System.out.println("텍스트 레이어가 없어 OCR로 전환합니다. (글자수 " + viaTextLayer.rawText().strip().length() + ")");

        // OCR은 페이지당 수십 초가 걸립니다. 정규식을 반복 조정할 때 매번 다시 돌리지 않도록 결과를 캐시합니다.
        Path cache = ocrCachePath();
        if (cache != null && Files.exists(cache)) {
            String cached = Files.readString(cache);
            System.out.println("캐시된 OCR 결과를 사용합니다. (" + cached.length() + "자, " + cache + ")");
            // 캐시에는 텍스트만 담기므로, 인식 신뢰도는 환경변수로 넘겨 실제 실행값을 재현합니다.
            String cachedConfidence = System.getenv("OCR_CONFIDENCE");
            double confidence = cachedConfidence == null || cachedConfidence.isBlank()
                    ? 1.0
                    : Double.parseDouble(cachedConfidence);
            return new ExtractedTextDto(cached, viaTextLayer.pageCount(), false, ExtractionSource.OCR, confidence);
        }

        long startedAt = System.currentTimeMillis();
        ExtractedTextDto viaOcr = new TesseractTextExtractor(tessdataPath,
                System.getenv("SCOUT_TESSDATA_PATH"),
                ocrDpi(),
                parallelism()).extract(pdfBytes, "application/pdf");
        System.out.println("OCR 전체 소요 " + (System.currentTimeMillis() - startedAt) / 1000 + "초");

        if (cache != null) {
            Files.createDirectories(cache.getParent());
            Files.writeString(cache, viaOcr.rawText());
            System.out.println("OCR 결과를 캐시했습니다: " + cache);
        }
        return viaOcr;
    }

    /** OCR 결과 캐시 파일 경로. 개인정보가 담기므로 저장소 바깥을 지정해야 합니다. */
    private Path ocrCachePath() {
        String path = System.getenv("OCR_TEXT_CACHE");
        return path == null || path.isBlank() ? null : Path.of(path);
    }

    private int parallelism() {
        String value = System.getenv("OCR_PARALLELISM");
        return value == null || value.isBlank() ? 0 : Integer.parseInt(value);
    }

    private int ocrDpi() {
        String dpi = System.getenv("OCR_DPI");
        return dpi == null || dpi.isBlank() ? 300 : Integer.parseInt(dpi);
    }

    private void printExtractedText(ExtractedTextDto extracted, boolean redact) {
        if (!redact) {
            System.out.println("=== 추출 원문 ===");
            System.out.println(extracted.rawText());
            return;
        }

        List<String> lines = Arrays.stream(extracted.rawText().split("\\R")).map(String::strip)
                .filter(line -> !line.isEmpty()).toList();
        List<String> relevant = lines.stream().filter(line -> DIAGNOSTIC_LINE.matcher(line).find()).toList();

        System.out.println("=== 추출 원문 (성적 관련 줄만) ===");
        System.out.println("전체 " + lines.size() + "줄 중 " + relevant.size() + "줄이 성적 표 형태로 보입니다.");
        System.out.println("나머지 줄은 개인정보가 포함될 수 있어 출력하지 않습니다.");
        System.out.println("전체 원문이 필요하면 RECORD_PDF_FULL_TEXT=true 로 다시 실행하세요.");
        System.out.println();
        relevant.stream().limit(80).forEach(System.out::println);
        if (relevant.size() > 80) {
            System.out.println("... (" + (relevant.size() - 80) + "줄 생략)");
        }
    }

    /** 성취점수를 생활기록부에 적힌 등급 문자로 되돌립니다. 원본과 눈으로 대조하기 위함입니다. */
    private String toGrade(Integer score) {
        if (score == null || score == 0) {
            return "-";
        }
        return switch (score) {
            case 5 -> "A";
            case 4 -> "B";
            case 3 -> "C";
            case 2 -> "D";
            default -> "E";
        };
    }

    private String row(String label, List<Integer> scores, int size) {
        StringBuilder line = new StringBuilder(String.format("%-5s|", label));
        for (int index = 0; index < size; index++) {
            Integer score = scores == null || index >= scores.size() ? null : scores.get(index);
            line.append(String.format("%5s", toGrade(score)));
        }
        return line.toString();
    }

    /** 읽지 못한 출결 · 봉사 값은 null이므로 빈칸으로 표시합니다. */
    private String cell(Integer value) {
        return value == null ? "-" : String.valueOf(value);
    }

    /** 실제 생활기록부와 대조할 수 있도록 학기 · 과목 표로 출력합니다. */
    private void printComparisonTable(ExtractedAchievementResDto result) {
        List<String> subjects = new ArrayList<>(result.achievement().generalSubjects());
        subjects.addAll(result.achievement().newSubjects());

        System.out.println();
        System.out.println("=== 일반교과 성취도 ===");
        StringBuilder header = new StringBuilder(String.format("%-5s|", "학기"));
        subjects.forEach(subject -> header.append(String.format("%5s", subject)));
        System.out.println(header);
        System.out.println("-".repeat(header.length()));
        System.out.println(row("1-1", result.achievement().achievement1_1(), subjects.size()));
        System.out.println(row("1-2", result.achievement().achievement1_2(), subjects.size()));
        System.out.println(row("2-1", result.achievement().achievement2_1(), subjects.size()));
        System.out.println(row("2-2", result.achievement().achievement2_2(), subjects.size()));
        System.out.println(row("3-1", result.achievement().achievement3_1(), subjects.size()));
        System.out.println(row("3-2", result.achievement().achievement3_2(), subjects.size()));

        System.out.println();
        System.out.println("=== 예체능 성취도 ===");
        List<String> artsSubjects = result.achievement().artsPhysicalSubjects();
        List<Integer> arts = result.achievement().artsPhysicalAchievement();
        if (arts != null && !artsSubjects.isEmpty()) {
            StringBuilder artsHeader = new StringBuilder(String.format("%-5s|", "학기"));
            artsSubjects.forEach(subject -> artsHeader.append(String.format("%5s", subject)));
            System.out.println(artsHeader);
            System.out.println("-".repeat(artsHeader.length()));
            for (int block = 0; block * artsSubjects.size() < arts.size(); block++) {
                int from = block * artsSubjects.size();
                System.out.println(row("#" + (block + 1),
                        arts.subList(from, Math.min(from + artsSubjects.size(), arts.size())),
                        artsSubjects.size()));
            }
            System.out.println("(#1부터 순서대로 대상 학기입니다)");
        }

        System.out.println();
        System.out.println("=== 출결 · 봉사 ===");
        System.out.println("학년 | 결석 | 지각 | 조퇴 | 결과 | 봉사시간");
        List<Integer> absent = result.achievement().absentDays();
        List<Integer> attendance = result.achievement().attendanceDays();
        List<Integer> volunteer = result.achievement().volunteerTime();
        for (int grade = 0; grade < 3; grade++) {
            System.out.printf("%4d | %4s | %4s | %4s | %4s | %8s%n",
                    grade + 1,
                    cell(absent.get(grade)),
                    cell(attendance.get(grade * 3)),
                    cell(attendance.get(grade * 3 + 1)),
                    cell(attendance.get(grade * 3 + 2)),
                    cell(volunteer.get(grade)));
        }
        System.out.println("(빈칸은 인식하지 못해 지원자가 직접 입력해야 하는 값입니다)");
    }

    private void printResult(ExtractedTextDto extracted, ExtractedAchievementResDto result, boolean redact) {
        System.out.println("=== 추출 진단 ===");
        System.out.println("hasTextLayer : " + extracted.hasTextLayer());
        System.out.println("인식신뢰도     : " + extracted.recognitionConfidence());
        System.out.println("source       : " + extracted.source());
        System.out.println("pageCount    : " + extracted.pageCount());
        System.out.println("글자수        : " + extracted.rawText().length());
        System.out.println();
        printExtractedText(extracted, redact);
        System.out.println();
        System.out.println("=== 파싱 결과 ===");
        System.out.println("generalSubjects  : " + result.achievement().generalSubjects());
        System.out.println("newSubjects      : " + result.achievement().newSubjects());
        System.out.println("achievement1_1   : " + result.achievement().achievement1_1());
        System.out.println("achievement1_2   : " + result.achievement().achievement1_2());
        System.out.println("achievement2_1   : " + result.achievement().achievement2_1());
        System.out.println("achievement2_2   : " + result.achievement().achievement2_2());
        System.out.println("achievement3_1   : " + result.achievement().achievement3_1());
        System.out.println("achievement3_2   : " + result.achievement().achievement3_2());
        System.out.println("artsPhysical     : " + result.achievement().artsPhysicalSubjects() + " → "
                + result.achievement().artsPhysicalAchievement());
        System.out.println("absentDays       : " + result.achievement().absentDays());
        System.out.println("attendanceDays   : " + result.achievement().attendanceDays());
        System.out.println("volunteerTime    : " + result.achievement().volunteerTime());
        System.out.println("confidence       : " + result.meta().confidence());
        System.out.println("unrecognized     : " + result.meta().unrecognizedSubjects());
        System.out.println("missingSemesters : " + result.meta().missingSemesters());
        System.out.println("warnings         : " + result.meta().warnings().size() + "건");
        result.meta().warnings().forEach(warning -> System.out.println("   - " + warning.type() + " " + warning.field()
                + (warning.index() == null ? "" : "[" + warning.index() + "]") + " : " + warning.message()));
    }

    @Test
    @DisplayName("PDF 생성 → 텍스트 추출 → 파싱 전체 경로가 성적을 복원한다")
    void it_runs_full_pipeline() throws IOException {
        String realPdfPath = System.getenv(REAL_PDF_ENV);
        boolean useRealPdf = realPdfPath != null && !realPdfPath.isBlank();

        Assumptions.assumeTrue(useRealPdf || new File(FONT_PATH).exists(),
                "한글 폰트를 찾을 수 없어 합성 PDF를 만들 수 없습니다: " + FONT_PATH);

        byte[] pdfBytes = useRealPdf ? Files.readAllBytes(Path.of(realPdfPath)) : createSyntheticRecordPdf();

        // 실제 파일은 졸업 구분에 따라 대상 학기가 달라지므로 환경변수로 지정할 수 있게 합니다.
        String graduationTypeEnv = System.getenv("GRADUATION_TYPE");
        GraduationType graduationType = graduationTypeEnv == null || graduationTypeEnv.isBlank()
                ? GraduationType.CANDIDATE
                : GraduationType.valueOf(graduationTypeEnv);

        ExtractedTextDto extracted = extractWithFallback(pdfBytes);
        ExtractedAchievementResDto result = new MiddleSchoolRecordParser(new SubjectNameNormalizer())
                .parse(extracted, graduationType, MiddleSchoolRecordParser.FREE_SEMESTER_SYSTEM);

        // 실제 생활기록부는 개인정보가 포함되므로 성적 관련 줄만 출력합니다.
        boolean redact = useRealPdf && !"true".equalsIgnoreCase(System.getenv(FULL_TEXT_ENV));
        printResult(extracted, result, redact);
        printComparisonTable(result);

        if (useRealPdf) {
            // 실제 생활기록부는 형식을 알 수 없으므로 출력만 하고 값을 단정하지 않습니다.
            return;
        }

        assertThat(extracted.hasTextLayer()).isTrue();
        assertThat(result.achievement().generalSubjects())
                .containsExactly("국어", "사회", "도덕", "역사", "수학", "과학", "기술가정", "정보", "영어");
        assertThat(result.achievement().newSubjects()).isEmpty();
        // 순서: 국어, 사회, 도덕, 역사, 수학, 과학, 기술가정, 정보, 영어
        assertThat(result.achievement().achievement1_2()).containsExactly(5, 4, 0, 0, 5, 0, 4, 5, 0);
        assertThat(result.achievement().achievement2_1()).containsExactly(5, 3, 0, 0, 2, 0, 0, 0, 0);
        assertThat(result.achievement().achievement3_1()).containsExactly(5, 4, 0, 0, 3, 0, 0, 0, 0);
        assertThat(result.achievement().artsPhysicalSubjects()).containsExactly("체육", "음악", "미술");
        // 자유학기제 졸업예정자: 1-1, 1-2, 2-1, 2-2, 3-1 → 15칸. 1-2의 체육 A가 index 3
        assertThat(result.achievement().artsPhysicalAchievement()).hasSize(15);
        assertThat(result.achievement().artsPhysicalAchievement().get(3)).isEqualTo(5);
        // 합성 PDF에는 1학년 출결 · 봉사만 담았으므로 나머지 학년은 비어 있어야 합니다.
        assertThat(result.achievement().absentDays()).containsExactly(2, null, null);
        assertThat(result.achievement().volunteerTime()).containsExactly(7, null, null);
    }
}
