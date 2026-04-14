package team.themoment.hellogsmv3.domain.common.utility.service;

import static team.themoment.hellogsmv3.domain.oneseo.service.OneseoService.ONESEO_CACHE_VALUE;

import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.AuthenticationCode;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType;
import team.themoment.hellogsmv3.domain.member.repository.CodeRepository;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
@Profile("!prod")
public class DeleteMemberService {

    private final MemberRepository memberRepository;
    private final OneseoRepository oneseoRepository;
    private final CodeRepository codeRepository;

    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = ONESEO_CACHE_VALUE, key = "#result")
    public Long execute(String phoneNumber) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ExpectedException("해당 전화 번호에 해당하는 계정이 존재하지 않습니다.", HttpStatus.NOT_FOUND));
        Long memberId = member.getId();
        Optional<Oneseo> oneseo = oneseoRepository.findByMember(member);
        oneseo.ifPresent(oneseoRepository::delete);
        deleteAuthenticationCodes(memberId);
        memberRepository.delete(member);
        return memberId;
    }

    private void deleteAuthenticationCodes(Long memberId) {
        for (AuthCodeType authCodeType : AuthCodeType.values()) {
            Optional<AuthenticationCode> authCode = codeRepository.findByMemberIdAndAuthCodeType(memberId,
                    authCodeType);
            authCode.ifPresent(codeRepository::delete);
        }
    }
}
