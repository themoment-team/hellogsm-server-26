package team.themoment.hellogsmv3.global.thirdParty.ocr;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITessAPI.TessPageIteratorLevel;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import net.sourceforge.tess4j.Word;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractionSource;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.AchievementTextExtractor;
import team.themoment.sdk.exception.ExpectedException;

/**
 * 페이지를 이미지로 렌더링한 뒤 광학 인식으로 텍스트를 추출합니다.
 *
 * <p>
 * 정부24 발급 생활기록부는 텍스트 레이어 없이 이미지로만 이루어져 있어 이 경로가 필요합니다. 텍스트 레이어 방식이 실패했을 때 서비스가
 * 이 구현체로 넘깁니다.
 *
 * <p>
 * 네이티브 Tesseract를 따로 설치할 필요는 없습니다. Windows용 DLL은 tess4j와 lept4j jar에 포함되어 있습니다.
 * 다만 언어 학습 데이터({@code kor.traineddata}, {@code eng.traineddata})는 별도로 준비해
 * {@code ocr.tessdata-path}로 지정해야 합니다.
 */
@Slf4j
@Order(2)
@Component
public class TesseractTextExtractor implements AchievementTextExtractor {

    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";

    /** 표 안의 작은 글씨를 읽으려면 이 정도 해상도가 필요합니다. 높일수록 정확하지만 느려집니다. */
    private static final int DEFAULT_DPI = 300;

    /** 한글 문서에도 숫자와 영문이 섞여 있어 함께 인식합니다. */
    private static final String LANGUAGE = "kor+eng";

    /** 균일한 텍스트 블록으로 가정합니다. 생활기록부는 표 위주라 이 모드가 적합합니다. */
    private static final int PAGE_SEG_MODE_SINGLE_BLOCK = 6;

    /** 페이지 선별용 해상도. 표식만 찾으면 되므로 낮게 잡습니다. */
    private static final int SCOUT_DPI = 100;

    /** 선별 단계도 항목 제목을 읽어야 하므로 한글이 필요합니다. 속도 우선 학습 데이터를 씁니다. */
    private static final String SCOUT_LANGUAGE = "kor+eng";

    /**
     * 성적 외에 읽어야 하는 항목의 제목입니다.
     *
     * <p>
     * 출결과 봉사 표에는 성취도 표식이 없어 표식만으로 고르면 페이지가 통째로 빠집니다. 실제로 봉사시간이 추출되지 않은 원인이 이것이었습니다.
     */
    private static final Pattern SECTION_KEYWORD = Pattern.compile("출결|봉사|체험활동");

    /** 성취도(수강자수) 표식. 예: {@code A(164)} */
    private static final Pattern ACHIEVEMENT_MARK = Pattern.compile("[A-E]\\s*\\(\\s*\\d{1,4}\\s*\\)");

    /**
     * 수강자수가 없는 성취도. 예체능 성적은 {@code A(164)}가 아니라 {@code A}로만 적혀 있어 별도로 셉니다.
     *
     * <p>
     * 줄 끝에 홀로 있는 A~E만 세어 본문 속 영문 약자와 구분합니다.
     */
    private static final Pattern BARE_ACHIEVEMENT_MARK = Pattern.compile("(?m)(?<![A-Za-z(])[A-E]\\s*$");

    /** 이 횟수 이상 표식이 나오면 성적 표가 실린 페이지로 봅니다. */
    private static final int MINIMUM_MARKS_PER_PAGE = 3;

    /** 표식이 발견된 구간을 앞뒤로 넓히는 폭. 예체능 행과 학년 구분 표시를 놓치지 않기 위함입니다. */
    private static final int PAGE_RANGE_MARGIN = 1;

    private final String tessdataPath;
    private final String scoutTessdataPath;
    private final int dpi;
    private final int parallelism;

    public TesseractTextExtractor(@Value("${ocr.tessdata-path:}") String tessdataPath,
            @Value("${ocr.scout-tessdata-path:}") String scoutTessdataPath,
            @Value("${ocr.dpi:" + DEFAULT_DPI + "}") int dpi,
            @Value("${ocr.parallelism:0}") int parallelism) {
        this.tessdataPath = tessdataPath;
        // 선별 단계는 표식만 찾으면 되므로 속도 우선 학습 데이터를 따로 지정할 수 있습니다.
        this.scoutTessdataPath = scoutTessdataPath == null || scoutTessdataPath.isBlank()
                ? tessdataPath
                : scoutTessdataPath;
        this.dpi = dpi;
        // 0이면 코어 수만큼 씁니다. 동시 요청이 많은 환경에서는 낮춰 잡아야 합니다.
        this.parallelism = parallelism > 0 ? parallelism : Runtime.getRuntime().availableProcessors();
    }

    @Override
    public boolean supports(String contentType) {
        return PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)
                || (contentType != null && contentType.toLowerCase().startsWith(IMAGE_CONTENT_TYPE_PREFIX));
    }

    @Override
    public ExtractedTextDto extract(byte[] content, String contentType) {
        ITesseract tesseract = createTesseract();

        if (PDF_CONTENT_TYPE.equalsIgnoreCase(contentType)) {
            return extractFromPdf(content, tesseract);
        }
        return extractFromImage(content, tesseract);
    }

    private ITesseract createTesseract() {
        Tesseract tesseract = new Tesseract();
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            tesseract.setDatapath(tessdataPath);
        }
        tesseract.setLanguage(LANGUAGE);
        tesseract.setPageSegMode(PAGE_SEG_MODE_SINGLE_BLOCK);
        // 숫자를 문자로 오인하는 것을 줄입니다.
        tesseract.setVariable("preserve_interword_spaces", "1");
        return tesseract;
    }

    private ExtractedTextDto extractFromPdf(byte[] content, ITesseract tesseract) {
        try (PDDocument document = Loader.loadPDF(content)) {
            if (document.isEncrypted()) {
                throw new ExpectedException("암호가 설정된 PDF는 처리할 수 없습니다. 암호를 해제한 뒤 다시 업로드해주세요.", HttpStatus.BAD_REQUEST);
            }

            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();
            List<Integer> targetPages = selectPages(renderer, pageCount);

            long startedAt = System.currentTimeMillis();
            List<PageResult> results = recognizePages(renderer, targetPages, dpi, this::createTesseract);
            String text = results.stream().map(PageResult::text)
                    .collect(java.util.stream.Collectors.joining(System.lineSeparator()));
            double confidence = meanConfidence(results);

            log.info("본문 인식 완료. 페이지수={}, 인식신뢰도={}, elapsedMs={}",
                    targetPages.size(),
                    Math.round(confidence * 100) / 100.0,
                    System.currentTimeMillis() - startedAt);
            return new ExtractedTextDto(text, pageCount, false, ExtractionSource.OCR, confidence);
        } catch (IOException e) {
            throw new ExpectedException("PDF 파일을 읽는 데 실패했습니다. 손상되지 않은 파일인지 확인해주세요.", HttpStatus.BAD_REQUEST);
        } catch (TesseractException e) {
            log.error("OCR 처리에 실패했습니다.", e);
            throw new ExpectedException("이미지에서 글자를 읽는 데 실패했습니다. 성적을 직접 입력해주세요.", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 성적 표가 실린 페이지만 고릅니다.
     *
     * <p>
     * 생활기록부는 20페이지 안팎이지만 교과 성적은 그중 두세 장에 몰려 있습니다. 전체를 고해상도로 읽으면 대부분의 시간을 세부능력특기사항처럼
     * 필요 없는 페이지에 씁니다.
     *
     * <p>
     * 그래서 낮은 해상도로 한 번 훑어 성취도 표식({@code A(164)} 형태)이 여러 번 나오는 페이지만 추립니다. 이 표식은 영문과
     * 숫자로만 되어 있어 저해상도에서도 잘 읽히고, 한글 학습 데이터를 쓰지 않아 탐색이 훨씬 빠릅니다.
     *
     * <p>
     * 추려진 페이지가 없으면 판단에 실패한 것으로 보고 전체 페이지를 처리합니다.
     */
    private List<Integer> selectPages(PDFRenderer renderer, int pageCount) throws IOException, TesseractException {
        long startedAt = System.currentTimeMillis();

        List<Integer> allPages = IntStream.range(0, pageCount).boxed().toList();
        List<PageResult> pageResults = recognizePages(renderer, allPages, SCOUT_DPI, this::createScout);

        List<Integer> selected = new ArrayList<>();
        for (int page = 0; page < pageResults.size(); page++) {
            String scoutText = pageResults.get(page).text();
            long marks = ACHIEVEMENT_MARK.matcher(scoutText).results().count()
                    + BARE_ACHIEVEMENT_MARK.matcher(scoutText).results().count();
            boolean hasSection = SECTION_KEYWORD.matcher(scoutText).find();
            if (marks >= MINIMUM_MARKS_PER_PAGE || hasSection) {
                selected.add(page);
            }
        }

        long elapsed = System.currentTimeMillis() - startedAt;
        if (selected.isEmpty()) {
            log.warn("성적 표가 있는 페이지를 찾지 못해 전체를 처리합니다. pageCount={}, elapsedMs={}", pageCount, elapsed);
            return IntStream.range(0, pageCount).boxed().toList();
        }

        List<Integer> expanded = expandToRange(selected, pageCount);
        log.info("페이지 선별 완료. 전체={}, 표식발견={}, 처리대상={}, elapsedMs={}",
                pageCount,
                selected.stream().map(page -> page + 1).toList(),
                expanded.stream().map(page -> page + 1).toList(),
                elapsed);
        return expanded;
    }

    /**
     * 표식이 발견된 페이지 구간을 앞뒤로 한 장씩 넓힙니다.
     *
     * <p>
     * 표식만으로 고르면 놓치는 것들이 실제로 확인되었습니다. 예체능 성적은 {@code A(164)}가 아니라 수강자수 없이
     * {@code A}로만 적혀 표식에 걸리지 않고, 학년 구분 표시가 앞 페이지 끝에 있으면 학년을 잘못 붙이게 됩니다. 교과 성적은 연속된
     * 페이지에 모여 있으므로 구간을 통째로 처리하는 편이 안전합니다.
     */
    private List<Integer> expandToRange(List<Integer> selected, int pageCount) {
        int from = Math.max(0, selected.get(0) - PAGE_RANGE_MARGIN);
        int to = Math.min(pageCount - 1, selected.get(selected.size() - 1) + PAGE_RANGE_MARGIN);
        return IntStream.rangeClosed(from, to).boxed().toList();
    }
    /**
     * 페이지를 렌더링하면서 병렬로 인식하고, 페이지 순서대로 결과를 돌려줍니다.
     *
     * <p>
     * 시간의 대부분은 인식에 들어가고 페이지끼리 서로 의존하지 않아 병렬화 효과가 큽니다. 다만 두 가지 제약이 있습니다.
     *
     * <ul>
     * <li>PDFBox 렌더러는 스레드 안전하지 않아 렌더링은 순차로 해야 합니다.</li>
     * <li>고해상도 페이지 하나가 수십 MB라 전부 미리 렌더링하면 메모리가 터집니다. 실제로 8장을 한꺼번에 올렸다가
     * OutOfMemoryError가 발생했습니다.</li>
     * </ul>
     *
     * <p>
     * 그래서 렌더링은 호출 스레드에서 순차로 하되, 동시에 살아있는 이미지 수를 병렬도만큼으로 제한합니다. 인식이 끝난 이미지는 바로
     * 회수됩니다.
     *
     * <p>
     * Tesseract 인스턴스도 스레드 안전하지 않아 스레드마다 하나씩 만들어 재사용합니다. 학습 데이터를 읽는 비용이 있어 작업마다 새로
     * 만들면 오히려 느려집니다.
     */
    private List<PageResult> recognizePages(PDFRenderer renderer,
            List<Integer> pages,
            int renderDpi,
            Supplier<ITesseract> factory) throws IOException, TesseractException {
        if (pages.size() == 1) {
            return List.of(recognizeOne(factory.get(), renderGray(renderer, pages.get(0), renderDpi)));
        }

        int threads = Math.min(pages.size(), parallelism);
        ThreadLocal<ITesseract> perThread = ThreadLocal.withInitial(factory);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        Semaphore inFlight = new Semaphore(threads);

        try {
            List<Future<PageResult>> futures = new ArrayList<>(pages.size());
            for (int page : pages) {
                inFlight.acquire();
                BufferedImage image = renderGray(renderer, page, renderDpi);
                futures.add(pool.submit(() -> {
                    try {
                        return recognizeOne(perThread.get(), image);
                    } finally {
                        inFlight.release();
                    }
                }));
            }

            List<PageResult> results = new ArrayList<>(futures.size());
            for (Future<PageResult> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TesseractException("인식이 중단되었습니다.", e);
        } catch (ExecutionException e) {
            throw new TesseractException("인식 중 오류가 발생했습니다.", e.getCause());
        } finally {
            pool.shutdown();
        }
    }

    /**
     * 회색조로 직접 렌더링합니다.
     *
     * <p>
     * Tesseract는 어차피 내부에서 회색조로 변환하므로, 처음부터 회색조로 그리면 변환 단계가 사라지고 메모리도 픽셀당 4바이트에서
     * 1바이트로 줄어듭니다.
     */
    private BufferedImage renderGray(PDFRenderer renderer, int page, int renderDpi) throws IOException {
        return renderer.renderImageWithDPI(page, renderDpi, ImageType.GRAY);
    }

    /**
     * 한 페이지를 인식하고 텍스트와 엔진이 보고한 신뢰도를 함께 돌려줍니다.
     *
     * <p>
     * {@code doOCR} 대신 줄 단위로 읽어 각 줄의 신뢰도를 받습니다. 인식 비용은 같지만, 엔진이 글자를 얼마나 확신했는지 알 수
     * 있습니다. 해상도가 낮아 글자가 뭉개지면 이 값이 떨어지므로, 그럴듯하지만 틀린 결과를 걸러내는 근거가 됩니다.
     */
    private PageResult recognizeOne(ITesseract tesseract, BufferedImage image) {
        List<Word> lines = tesseract.getWords(image, TessPageIteratorLevel.RIL_TEXTLINE);

        StringBuilder text = new StringBuilder();
        double weightedConfidence = 0;
        int totalLength = 0;

        for (Word line : lines) {
            String lineText = line.getText() == null ? "" : line.getText().strip();
            if (lineText.isEmpty()) {
                continue;
            }
            text.append(lineText).append(System.lineSeparator());
            // 긴 줄이 더 큰 비중을 갖도록 글자 수로 가중 평균합니다.
            weightedConfidence += line.getConfidence() * lineText.length();
            totalLength += lineText.length();
        }

        double confidence = totalLength == 0 ? 0 : weightedConfidence / totalLength / 100;
        return new PageResult(text.toString(), Math.clamp(confidence, 0, 1), totalLength);
    }

    /** 페이지별 신뢰도를 글자 수로 가중 평균합니다. */
    private double meanConfidence(List<PageResult> results) {
        int totalLength = results.stream().mapToInt(PageResult::length).sum();
        if (totalLength == 0) {
            return 0;
        }
        return results.stream().mapToDouble(result -> result.confidence() * result.length()).sum() / totalLength;
    }

    /** 한 페이지의 인식 결과입니다. */
    private record PageResult(String text, double confidence, int length) {
    }

    private ITesseract createScout() {
        Tesseract scout = new Tesseract();
        if (scoutTessdataPath != null && !scoutTessdataPath.isBlank()) {
            scout.setDatapath(scoutTessdataPath);
        }
        scout.setLanguage(SCOUT_LANGUAGE);
        scout.setPageSegMode(PAGE_SEG_MODE_SINGLE_BLOCK);
        return scout;
    }

    private ExtractedTextDto extractFromImage(byte[] content, ITesseract tesseract) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(content));
            if (image == null) {
                throw new ExpectedException("이미지를 읽을 수 없습니다. 다른 파일로 시도해주세요.", HttpStatus.BAD_REQUEST);
            }
            PageResult result = recognizeOne(tesseract, image);
            return new ExtractedTextDto(result.text(), 1, false, ExtractionSource.OCR, result.confidence());
        } catch (IOException e) {
            throw new ExpectedException("이미지 파일을 읽는 데 실패했습니다.", HttpStatus.BAD_REQUEST);
        }
    }
}
