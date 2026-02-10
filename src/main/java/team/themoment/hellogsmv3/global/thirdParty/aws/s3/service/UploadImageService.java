package team.themoment.hellogsmv3.global.thirdParty.aws.s3.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Exception;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.data.S3Environment;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.dto.response.UploadImageResDto;

@Service
@RequiredArgsConstructor
public class UploadImageService {

    private final S3Template s3Template;
    private final S3Environment s3Environment;

    public UploadImageResDto execute(MultipartFile multipartFile) {
        if (multipartFile.isEmpty()) {
            throw new ExpectedException("파일이 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        String originalFileName = multipartFile.getOriginalFilename();
        String fileExtension = StringUtils.getFilenameExtension(originalFileName);
        validateFileContentType(Objects.requireNonNull(fileExtension));
        String fileName = generateFileName(fileExtension);

        try {
            S3Resource s3Resource = s3Template.upload(s3Environment.bucketName(),
                    fileName,
                    multipartFile.getInputStream(),
                    ObjectMetadata.builder().contentType(multipartFile.getContentType()).build());

            return new UploadImageResDto(s3Resource.getURL().toString());
        } catch (IOException e) {
            throw new RuntimeException("입출력 작업중에 예외 발생", e);
        } catch (S3Exception e) {
            throw new RuntimeException("S3에서 예외 발생", e);
        } catch (AwsServiceException e) {
            throw new RuntimeException("AWS에서 예외 발생", e);
        } catch (SdkClientException e) {
            throw new RuntimeException("클라이언측 문제로 인한 예외 발생", e);
        }
    }

    private String generateFileName(String fileExtension) {
        return UUID.randomUUID().toString() + LocalDateTime.now() + "." + fileExtension;
    }

    private void validateFileContentType(String fileExtension) {
        List<String> allowExtension = List.of("jpg", "jpeg", "png");
        if (!allowExtension.contains(fileExtension.toLowerCase())) {
            throw new ExpectedException("지원하지 않는 파일 확장자 입니다.", HttpStatus.BAD_REQUEST);
        }
    }
}
