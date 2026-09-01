package team.themoment.hellogsmv3.domain.member.service.impl;

import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.*;

import java.util.Random;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.dto.request.GenerateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;
import team.themoment.hellogsmv3.domain.member.service.GenerateCodeService;

@Service
@RequiredArgsConstructor
public class GenerateTestCodeServiceImpl extends GenerateCodeService {

    private final CodeRepository codeRepository;
    private static final Random RANDOM = new Random();

    @Override
    public String execute(Long memberId, GenerateCodeReqDto reqDto) {
        final String code = generateUniqueCode(RANDOM, codeRepository);

        AuthenticationCode authenticationCode = codeRepository.findByMemberIdAndAuthCodeType(memberId, SIGNUP)
                .orElse(null);

        codeRepository
                .save(createAuthenticationCode(authenticationCode, memberId, code, reqDto.phoneNumber(), SIGNUP, true));

        return code;
    }
}
