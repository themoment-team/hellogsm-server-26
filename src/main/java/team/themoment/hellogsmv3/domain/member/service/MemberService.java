package team.themoment.hellogsmv3.domain.member.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    public Member findByIdOrThrow(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(
                () -> new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
    }

    public Member findByIdForUpdateOrThrow(Long memberId) {
        return memberRepository.findByIdForUpdate(memberId).orElseThrow(
                () -> new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));
    }
}
