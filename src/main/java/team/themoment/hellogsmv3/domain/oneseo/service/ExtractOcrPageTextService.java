package team.themoment.hellogsmv3.domain.oneseo.service;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocParseResult;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrPageTextResDto;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocAchievementTextConverter;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocConversionResult;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.KordocOcrClient;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr.OcrConcurrencyGate;
import team.themoment.sdk.exception.ExpectedException;

/**
 * 텍스트 레이어가 없는 생활기록부 페이지의 스캔 이미지를 kordoc으로 인식해, 기존
 * {@link team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser}가
 * 읽는 rawText 줄 형식으로 되돌립니다.
 *
 * <p>
 * 업로드된 이미지는 kordoc 실행을 위해 임시 파일로만 잠깐 존재하고, 처리 직후(성공 · 실패 무관) 삭제되며 서버에 영구 저장되지
 * 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExtractOcrPageTextService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    // kordoc(OCR 엔진) 프로세스의 메모리 사용량은 이미지 해상도에 비례한다. 스캔 이미지는 텍스트 인식에
    // 필요한 해상도보다 훨씬 크게 올라오는 경우가 많아, 긴 변을 이 값으로 제한해 메모리 사용량을 낮춘다.
    private static final int MAX_DIMENSION_PX = 2000;

    private final KordocOcrClient kordocOcrClient;
    private final KordocAchievementTextConverter converter;
    private final OcrConcurrencyGate concurrencyGate;

    public OcrPageTextResDto execute(MultipartFile file) {
        validate(file);

        Path tempImage = writeToTempFile(file);
        try {
            KordocParseResult parseResult = concurrencyGate.runWithinLimit(() -> kordocOcrClient.recognize(tempImage));
            KordocConversionResult conversion = converter.convert(parseResult);

            if (!conversion.unrecognizedSubjectBlobs().isEmpty()) {
                log.warn("OCR 페이지에서 과목명 분해 실패. unrecognizedSubjectBlobCount={}",
                        conversion.unrecognizedSubjectBlobs().size());
            }

            return OcrPageTextResDto.builder().rawText(conversion.rawText())
                    .unrecognizedSubjectBlobs(conversion.unrecognizedSubjectBlobs()).build();
        } finally {
            deleteQuietly(tempImage);
        }
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ExpectedException("파일이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new ExpectedException("지원하지 않는 파일 확장자입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    private Path writeToTempFile(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        Path tempFile;
        try {
            tempFile = Files.createTempFile("ocr-page-", "." + extension);
        } catch (IOException e) {
            throw new ExpectedException("업로드된 이미지를 처리하지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        restrictToOwner(tempFile);
        try {
            file.transferTo(tempFile);
            downscaleIfNeeded(tempFile, extension);
            return tempFile;
        } catch (IOException e) {
            deleteQuietly(tempFile);
            throw new ExpectedException("업로드된 이미지를 처리하지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /** 다운스케일은 메모리 최적화일 뿐 필수 동작이 아니므로, 실패해도 원본 해상도로 OCR을 계속 진행한다. */
    private void downscaleIfNeeded(Path imagePath, String extension) {
        BufferedImage original;
        try {
            original = ImageIO.read(imagePath.toFile());
        } catch (IOException e) {
            log.warn("스캔 이미지 다운스케일을 위한 읽기 실패, 원본 해상도로 진행합니다. path={}", imagePath);
            return;
        }
        if (original == null) {
            return;
        }

        int width = original.getWidth();
        int height = original.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= MAX_DIMENSION_PX) {
            return;
        }

        double scale = (double) MAX_DIMENSION_PX / longestSide;
        int scaledWidth = Math.max(1, (int) Math.round(width * scale));
        int scaledHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaled.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics.drawImage(original, 0, 0, scaledWidth, scaledHeight, null);
        graphics.dispose();

        String formatName = "png".equalsIgnoreCase(extension) ? "png" : "jpg";
        try {
            ImageIO.write(scaled, formatName, imagePath.toFile());
        } catch (IOException e) {
            log.warn("스캔 이미지 다운스케일 결과 저장 실패, 원본 해상도로 진행합니다. path={}", imagePath);
        }
    }

    /** 업로드된 스캔 이미지는 생활기록부 실물이므로, 임시 파일을 소유자만 읽고 쓸 수 있게 제한합니다. */
    private void restrictToOwner(Path file) {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        try {
            Files.setPosixFilePermissions(file,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (IOException e) {
            log.warn("업로드 임시 이미지 권한 설정 실패. path={}", file);
        }
    }

    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("OCR 임시 이미지 삭제 실패. path={}", file);
        }
    }
}
