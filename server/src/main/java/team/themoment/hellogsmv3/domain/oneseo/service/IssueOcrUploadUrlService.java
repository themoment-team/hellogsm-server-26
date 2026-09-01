package team.themoment.hellogsmv3.domain.oneseo.service;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.OcrUploadUrlResDto;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.data.S3Environment;
import team.themoment.sdk.exception.ExpectedException;

@Service
@RequiredArgsConstructor
public class IssueOcrUploadUrlService {

    private static final String OBJECT_KEY_PREFIX = "ocr-uploads/";
    private static final Duration UPLOAD_URL_EXPIRATION = Duration.ofMinutes(5);
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "jpg", "jpeg", "png");

    private final AuthorizeOcrService authorizeOcrService;
    private final S3Template s3Template;
    private final S3Environment s3Environment;

    public OcrUploadUrlResDto execute(Long memberId, String fileExtension) {
        String normalizedExtension = fileExtension.toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(normalizedExtension)) {
            throw new ExpectedException("지원하지 않는 파일 확장자입니다.", HttpStatus.BAD_REQUEST);
        }

        authorizeOcrService.execute(memberId);

        String objectKey = OBJECT_KEY_PREFIX + memberId + "/" + UUID.randomUUID() + "." + normalizedExtension;
        URL uploadUrl = s3Template.createSignedPutURL(s3Environment.bucketName(), objectKey, UPLOAD_URL_EXPIRATION);

        return new OcrUploadUrlResDto(uploadUrl.toString(), objectKey);
    }
}
