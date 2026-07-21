package team.themoment.hellogsmv3.domain.member.service;

import java.time.LocalDateTime;
import java.util.Random;

import team.themoment.hellogsmv3.domain.member.dto.request.GenerateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;

public abstract class GenerateCodeService {
    protected static final int DIGIT_NUMBER = 6;
    protected static final int LIMIT_COUNT_CODE_REQUEST = 5;
    protected static final int MAX = (int) Math.pow(10, DIGIT_NUMBER) - 1;

    protected abstract String execute(Long memberId, GenerateCodeReqDto reqDto);

    protected AuthenticationCode createAuthenticationCode(AuthenticationCode authCode,
            Long memberId,
            String code,
            String phoneNumber,
            AuthCodeType authCodeType,
            boolean isTest) {

        return authCode == null
                ? new AuthenticationCode(memberId, code, phoneNumber, LocalDateTime.now(), authCodeType, isTest)
                : authCode.updatedCode(code, LocalDateTime.now(), isTest);
    }

    protected String generateUniqueCode(Random RANDOM, CodeRepository codeRepository) {
        String code;
        do {
            code = getRandomCode(RANDOM);
        } while (isDuplicate(code, codeRepository));
        return code;
    }

    protected Boolean isDuplicate(String code, CodeRepository codeRepository) {
        return codeRepository.findByCode(code).isPresent();
    }

    protected String getRandomCode(Random RANDOM) {
        return String.format("%0" + DIGIT_NUMBER + "d", RANDOM.nextInt(0, MAX + 1));
    }
}
