package team.themoment.hellogsmv3.domain.member.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class CommonCodeService {

    private final CodeRepository codeRepository;

    public void validateAndDelete(Long memberId, String inputCode, String inputPhoneNumber, AuthCodeType authCodeType) {
        AuthenticationCode code = codeRepository.findByMemberIdAndAuthCodeType(memberId, authCodeType).orElseThrow(
                () -> new ExpectedException("사용자의 code가 존재하지 않습니다. 사용자의 ID : " + memberId, HttpStatus.BAD_REQUEST));

        if (!code.getAuthenticated()) {
            throw new ExpectedException("유효하지 않은 요청입니다. 인증받지 않은 code입니다.", HttpStatus.BAD_REQUEST);
        }

        if (!code.getCode().equals(inputCode)) {
            throw new ExpectedException("유효하지 않은 요청입니다. 이전 혹은 잘못된 형식의 code입니다.", HttpStatus.BAD_REQUEST);
        }

        if (!code.getPhoneNumber().equals(inputPhoneNumber)) {
            throw new ExpectedException("유효하지 않은 요청입니다. code인증에 사용되었던 전화번호와 요청에 사용한 전화번호가 일치하지 않습니다.",
                    HttpStatus.BAD_REQUEST);
        }

        codeRepository.delete(code);
    }
}
