package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocParseResult;
import team.themoment.sdk.exception.ExpectedException;

/**
 * kordoc(Node.js CLI, {@code @clazic/kordoc})을 subprocess로 실행합니다.
 *
 * <p>
 * 별도 서비스를 두지 않고 {@link ProcessBuilder}로 직접 실행하는 방식(연동 방식 A)입니다. 트래픽이 늘어 프로세스 기동
 * 오버헤드가 문제가 되면 별도 Node 마이크로서비스(방식 B)로 전환을 검토합니다.
 *
 * <p>
 * <b>검증 한계:</b> 실행 환경에 kordoc이 설치되어 있지 않아 이 클래스는 실제 kordoc 실행으로 검증하지 못했습니다. 배포
 * 전 실제 Node 환경에서 한 번 실행해 CLI 인자 · 종료 코드 · JSON 출력 형태를 확인해야 합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KordocCliOcrClient implements KordocOcrClient {

    private final ObjectMapper objectMapper;

    @Value("${oneseo.extraction.ocr.process-timeout-seconds:30}")
    private long processTimeoutSeconds;

    @Override
    public KordocParseResult recognize(Path imagePath) {
        Path stdoutFile = null;
        Path stderrFile = null;
        try {
            stdoutFile = Files.createTempFile("kordoc-out-", ".json");
            stderrFile = Files.createTempFile("kordoc-err-", ".log");
            restrictToOwner(stdoutFile);
            restrictToOwner(stderrFile);

            // npx가 로컬에 kordoc을 찾지 못하면(주로 개발 환경) 설치 여부를 묻는 프롬프트로 멈출 수 있어 -y로 자동 동의합니다.
            Process process = new ProcessBuilder("npx",
                    "-y",
                    "kordoc",
                    imagePath.toString(),
                    "--format",
                    "json",
                    "--ocr").redirectOutput(stdoutFile.toFile()).redirectError(stderrFile.toFile()).start();
            // 기본 표준 입력(PIPE)을 그대로 두면 kordoc이 입력을 기다릴 때 무한정 블록될 수 있어 즉시 EOF를 보냅니다.
            closeQuietly(process.getOutputStream());

            boolean finished = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ExpectedException("OCR 처리 시간이 너무 오래 걸려 중단했습니다. 다시 시도해주세요.", HttpStatus.GATEWAY_TIMEOUT);
            }
            if (process.exitValue() != 0) {
                log.error("kordoc 실행 실패. exitCode={}, stderr={}", process.exitValue(), readQuietly(stderrFile));
                throw new ExpectedException("스캔 이미지를 인식하지 못했습니다. 다른 이미지로 시도해주세요.", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            return parseJson(Files.readString(stdoutFile, StandardCharsets.UTF_8));
        } catch (IOException e) {
            log.error("kordoc 프로세스 실행 중 입출력 오류", e);
            throw new ExpectedException("OCR 엔진을 실행하지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExpectedException("OCR 처리가 중단되었습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            deleteQuietly(stdoutFile);
            deleteQuietly(stderrFile);
        }
    }

    KordocParseResult parseJson(String json) {
        try {
            return objectMapper.readValue(json, KordocParseResult.class);
        } catch (JsonProcessingException e) {
            log.error("kordoc 출력 JSON 파싱 실패", e);
            throw new ExpectedException("OCR 결과를 해석하지 못했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String readQuietly(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * kordoc의 출력에는 생활기록부에서 인식한 실제 내용(개인정보)이 담기므로, 임시 파일을 소유자만 읽고 쓸 수 있게 제한합니다.
     * POSIX를 지원하지 않는 파일 시스템(로컬 Windows 개발 환경 등)에서는 건너뜁니다.
     */
    private void restrictToOwner(Path file) {
        if (!FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            return;
        }
        try {
            Files.setPosixFilePermissions(file,
                    Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (IOException e) {
            log.warn("kordoc 임시 파일 권한 설정 실패. path={}", file);
        }
    }

    private void closeQuietly(OutputStream stream) {
        try {
            stream.close();
        } catch (IOException e) {
            log.warn("kordoc 표준 입력 스트림 종료 실패");
        }
    }

    private void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("kordoc 임시 파일 삭제 실패. path={}", file);
        }
    }
}
