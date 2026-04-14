package team.themoment.hellogsmv3.domain.member.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.dto.response.FoundMemberResDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@Service
@RequiredArgsConstructor
public class QueryMemberByIdService {

    private final MemberRepository memberRepository;

    @Transactional(readOnly = true)
    public FoundMemberResDto execute(Long memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(
                () -> new ExpectedException("존재하지 않는 지원자입니다. member ID: " + memberId, HttpStatus.NOT_FOUND));

        return FoundMemberResDto.builder().memberId(member.getId()).name(member.getName())
                .phoneNumber(member.getPhoneNumber()).birth(member.getBirth()).sex(member.getSex()).build();
    }
}
