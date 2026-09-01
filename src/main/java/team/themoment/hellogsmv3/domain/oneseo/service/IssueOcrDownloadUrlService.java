package team.themoment.hellogsmv3.domain.oneseo.service;

import java.net.URL;
import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    private final S3Template s3Template;
    private final S3Environment s3Environment;

    public OcrDownloadUrlResDto execute(Long memberId, String objectKey) {
        String ownedPrefix = OBJECT_KEY_PREFIX + memberId + "/";
        if (!objectKey.startsWith(ownedPrefix)) {
            throw new ExpectedException("접근할 수 없는 파일입니다.", HttpStatus.FORBIDDEN);
        }

        if (!s3Template.objectExists(s3Environment.bucketName(), objectKey)) {
            throw new ExpectedException("존재하지 않거나 만료된 파일입니다. objectKey: " + objectKey, HttpStatus.NOT_FOUND);
        }

        URL downloadUrl = s3Template.createSignedGetURL(s3Environment.bucketName(), objectKey, DOWNLOAD_URL_EXPIRATION);

        return new OcrDownloadUrlResDto(downloadUrl.toString());
    }
}
