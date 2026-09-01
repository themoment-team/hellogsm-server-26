package team.themoment.hellogsmv3.domain.oneseo.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrDownloadUrlResDto;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.data.S3Environment;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class IssueOcrDownloadUrlService {

    private static final String OBJECT_KEY_PREFIX = "ocr-uploads/";
    private static final Duration DOWNLOAD_URL_EXPIRATION = Duration.ofMinutes(1);
    private static final long MAX_OBJECT_SIZE_BYTES = 30L * 1024 * 1024;

    private final S3Template s3Template;
    private final S3Environment s3Environment;

    public OcrDownloadUrlResDto execute(Long memberId, String objectKey) {
        String ownedPrefix = OBJECT_KEY_PREFIX + memberId + "/";
        if (!objectKey.startsWith(ownedPrefix)) {
            throw new ExpectedException("접근할 수 없는 파일입니다.", HttpStatus.FORBIDDEN);
        }

        S3Resource resource = s3Template.createResource(s3Environment.bucketName(), objectKey);
        if (!resource.exists()) {
            throw new ExpectedException("존재하지 않거나 만료된 파일입니다. objectKey: " + objectKey, HttpStatus.NOT_FOUND);
        }

        if (resource.contentLength() > MAX_OBJECT_SIZE_BYTES) {
            s3Template.deleteObject(s3Environment.bucketName(), objectKey);
            throw new ExpectedException("업로드 가능한 최대 용량(30MB)을 초과한 파일입니다. objectKey: " + objectKey,
                    HttpStatus.CONTENT_TOO_LARGE);
        }

        URL downloadUrl = s3Template.createSignedGetURL(s3Environment.bucketName(), objectKey, DOWNLOAD_URL_EXPIRATION);

        return new OcrDownloadUrlResDto(downloadUrl.toString());
    }
}
