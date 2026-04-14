package team.themoment.hellogsmv3.domain.member.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.dto.request.AuthenticateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class AuthenticateCodeService {

    private final CodeRepository codeRepository;

    @Transactional
    public void execute(Long memberId, AuthenticateCodeReqDto reqDto, AuthCodeType authCodeType) {
        AuthenticationCode code = codeRepository.findByMemberIdAndAuthCodeType(memberId, authCodeType).orElseThrow(
                () -> new ExpectedException("사용자의 code가 존재하지 않습니다. member ID : " + memberId, HttpStatus.NOT_FOUND));

        if (!code.getCode().equals(reqDto.code()))
            throw new ExpectedException("유효하지 않은 code 이거나 이전 혹은 잘못된 code 입니다.", HttpStatus.BAD_REQUEST);

        code.authenticatedAuthenticationCode();

        codeRepository.save(code);
    }
}
