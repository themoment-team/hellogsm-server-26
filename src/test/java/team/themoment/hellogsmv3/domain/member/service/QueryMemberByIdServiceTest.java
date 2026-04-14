package team.themoment.hellogsmv3.domain.member.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import team.themoment.hellogsmv3.domain.member.dto.response.FoundMemberResDto;
import team.themoment.hellogsmv3.domain.member.entity.Member;
import team.themoment.hellogsmv3.domain.member.entity.type.Sex;
import team.themoment.hellogsmv3.domain.member.repository.MemberRepository;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;

@DisplayName("QueryMemberByIdService 클래스의")
class QueryMemberByIdServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private QueryMemberByIdService queryMemberByIdService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메소드는")
    class Describe_execute {

        private final Long memberId = 1L;

        @Nested
        @DisplayName("존재하는 회원 ID가 주어지면")
        class Context_with_existing_member_id {

            private Member member;

            @BeforeEach
            void setUp() {
                member = Member.builder().id(memberId).name("최장우").birth(LocalDate.of(2006, 3, 6))
                        .phoneNumber("01012345678").sex(Sex.MALE).build();

                given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
            }

            @Test
            @DisplayName("회원 정보를 반환한다")
            void it_returns_member_info() {
                FoundMemberResDto result = queryMemberByIdService.execute(memberId);

                assertEquals(member.getId(), result.memberId());
                assertEquals(member.getName(), result.name());
                assertEquals(member.getBirth(), result.birth());
                assertEquals(member.getPhoneNumber(), result.phoneNumber());
                assertEquals(member.getSex(), result.sex());
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
            @DisplayName("ExpectedException을 던진다")
            void it_throws_expected_exception() {
                ExpectedException exception = assertThrows(ExpectedException.class, () -> {
                    queryMemberByIdService.execute(memberId);
                });

                assertEquals("존재하지 않는 지원자입니다. member ID: " + memberId, exception.getMessage());
                assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
            }
        }
    }
}
