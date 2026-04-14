package team.themoment.hellogsmv3.domain.common.utility.service;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
@Profile("!prod")
public class ModifyMemberRoleService {

    private final MemberRepository memberRepository;

    @Transactional
    public void execute(String phoneNumber, Role role) {
        Member member = memberRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ExpectedException("해당 전화 번호에 해당하는 계정이 존재하지 않습니다.", HttpStatus.NOT_FOUND));
        member.modifyMemberRole(role);
    }
}
