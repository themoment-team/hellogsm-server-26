package team.themoment.hellogsmv3.domain.member.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import team.themoment.hellogsmv3.domain.member.dto.response.FoundDuplicateMemberResDto;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;

@DisplayName("QueryCheckDuplicateMemberService 클래스의")
class QueryCheckDuplicateMemberServiceTest {
    @Mock
    private MemberRepository memberRepository;
    @InjectMocks
    private QueryCheckDuplicateMemberService queryCheckDuplicateMemberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {
        private final String phoneNumber = "01038157596";

        @Nested
        @DisplayName("파라미터로 받은 전화번호가 주어졌을 때")
        class Context_with_phone_number {
            @Test
            @DisplayName("중복된 전화번호라면 YES를 반환한다")
            void it_duplicate_return_yes() {
                // given
                given(memberRepository.existsByPhoneNumber(phoneNumber)).willReturn(true);
                // when
                FoundDuplicateMemberResDto resDto = queryCheckDuplicateMemberService.execute(phoneNumber);
                // then
                assertEquals(YES, resDto.duplicateMemberYn());
            }

            @Test
            @DisplayName("중복된 전화번호가 아니라면 NO를 반환한다")
            void it_not_duplicate_return_no() {
                // given
                given(memberRepository.existsByPhoneNumber(phoneNumber)).willReturn(false);
                // when
                FoundDuplicateMemberResDto resDto = queryCheckDuplicateMemberService.execute(phoneNumber);
                // then
                assertEquals(NO, resDto.duplicateMemberYn());
            }
        }
    }
}
