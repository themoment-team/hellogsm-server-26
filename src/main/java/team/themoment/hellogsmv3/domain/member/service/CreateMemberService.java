package team.themoment.hellogsmv3.domain.member.service;

import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.*;
import static team.themoment.hellogsmv3.domain.member.entity.type.Role.*;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import team.themoment.hellogsmv3.domain.member.dto.request.CreateMemberReqDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.domain.oneseo.repository.*;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.data.ScheduleEnvironment;

@Service
@Slf4j
@RequiredArgsConstructor
public class CreateMemberService {

    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final CommonCodeService commonCodeService;
    private final ScheduleEnvironment scheduleEnv;

    private final OneseoRepository oneseoRepository;
    private final EntranceTestResultRepository entranceTestResultRepository;

    @Transactional
    public Role execute(CreateMemberReqDto reqDto, Long memberId) {

        commonCodeService.validateAndDelete(memberId, reqDto.code(), reqDto.phoneNumber(), SIGNUP);

        Member member = memberService.findByIdOrThrow(memberId);

        if (member.getRole().equals(APPLICANT))
            throw new ExpectedException("이미 회원가입 하였습니다.", HttpStatus.BAD_REQUEST);

        ifDuplicateMemberDeleteMemberInfo(reqDto.phoneNumber());

        Member newMember = buildMember(reqDto, member);
        memberRepository.save(newMember);

        return newMember.getRole();
    }

    private void ifDuplicateMemberDeleteMemberInfo(String phoneNumber) {
        if (LocalDateTime.now().isAfter(scheduleEnv.oneseoSubmissionEnd())
                || entranceTestResultRepository.existsByFirstTestPassYnIsNotNull())
            throw new ExpectedException("원서접수 기간 이후에는 기존 회원정보를 삭제하고 회원가입할 수 없습니다.", HttpStatus.BAD_REQUEST);

        memberRepository.findByPhoneNumber(phoneNumber).ifPresent(duplicateMember -> {
            log.warn("중복된 전화번호({})로 회원가입 요청이 되어 기존 회원정보를 삭제합니다.", phoneNumber);
            deleteCascadeOneseo(duplicateMember);
            deleteMember(duplicateMember);
        });
    }

    private void deleteCascadeOneseo(Member duplicateMember) {
        oneseoRepository.findByMember(duplicateMember).ifPresent(oneseoRepository::delete);
    }

    private void deleteMember(Member duplicateMember) {
        memberRepository.deleteDuplicate(duplicateMember);
    }

    private Member buildMember(CreateMemberReqDto reqDto, Member member) {
        return Member.builder().id(member.getId()).email(member.getEmail())
                .authReferrerType(member.getAuthReferrerType()).name(reqDto.name()).birth(reqDto.birth())
                .phoneNumber(reqDto.phoneNumber()).sex(reqDto.sex()).role(APPLICANT).build();
    }
}
