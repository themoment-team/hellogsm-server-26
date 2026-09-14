package team.themoment.hellogsmv3.global.thirdParty.aws.s3.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.dto.response.UploadImageResDto;
import team.themoment.hellogsmv3.global.thirdParty.aws.s3.service.UploadImageService;

@Tag(name = "ThirdParty API", description = "증명사진 등록 관련 API입니다.")
@RestController
@RequestMapping("/oneseo/v3")
@RequiredArgsConstructor
public class S3Controller {

    private final UploadImageService uploadImageService;

    @Operation(summary = "증명사진 등록", description = "증명사진 이미지를 요청받아 등록합니다.")
    @PostMapping("/image")
    public UploadImageResDto uploadImage(@RequestParam(value = "file") MultipartFile multipartFile) {
        return uploadImageService.execute(multipartFile);
    }
}
