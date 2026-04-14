package team.themoment.hellogsmv3.domain.member.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("MemberService 클래스의")
public class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("findByIdOrThrow 메소드는")
    class Describe_findByIdOrThrow {

        private final Long memberId = 1L;
        private final Member member = Member.builder().id(memberId).build();

        @Nested
        @DisplayName("존재하는 회원 ID가 주어지면")
        class Context_with_existing_member_id {

            @BeforeEach
            void setUp() {
                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            }

            @Test
            @DisplayName("회원 정보를 반환한다.")
            void it_returns_member() {
                Member foundMember = memberService.findByIdOrThrow(memberId);
                assertEquals(member, foundMember);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 회원 ID가 주어지면")
        class Context_with_non_existing_member_id {

            @BeforeEach
            void setUp() {
                given(memberRepository.findById(memberId)).willReturn(Optional.empty());
            }

            @Test
            @DisplayName("ExpectedException을 던진다.")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class,
                        () -> memberService.findByIdOrThrow(memberId));

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
