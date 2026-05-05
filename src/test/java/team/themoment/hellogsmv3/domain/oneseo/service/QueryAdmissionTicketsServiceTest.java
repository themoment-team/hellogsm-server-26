package team.themoment.hellogsmv3.domain.oneseo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import team.themoment.hellogsmv3.domain.oneseo.dto.response.AdmissionTicketsResDto;
import team.themoment.hellogsmv3.domain.oneseo.repository.custom.CustomOneseoRepository;

@DisplayName("QueryAdmissionTicketsService 클래스의")
class QueryAdmissionTicketsServiceTest {

    @Mock
    private CustomOneseoRepository customOneseoRepository;

    @InjectMocks
    private QueryAdmissionTicketsService queryAdmissionTicketsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Nested
    @DisplayName("execute 메서드는")
    class Describe_execute {
        AdmissionTicketsResDto firstadmissionTicketsResDto = AdmissionTicketsResDto.builder().memberName("홍길동")
                .memberBirth(LocalDate.parse("2024-07-28")).profileImg("profileImg.com").schoolName("광주소프트웨어마이스터고등학교")
                .examinationNumber("1010").oneseoSubmitCode("A-1").build();

        AdmissionTicketsResDto secontadmissionTicketsResDto = AdmissionTicketsResDto.builder().memberName("홍길동")
                .memberBirth(LocalDate.parse("2024-07-28")).profileImg("profileImg.com").schoolName("광주소프트웨어마이스터고등학교")
                .examinationNumber("1020").oneseoSubmitCode("A-1").build();

        List<AdmissionTicketsResDto> expectedTickets = Arrays.asList(firstadmissionTicketsResDto,
                secontadmissionTicketsResDto);

        @BeforeEach
        void setUp() {
            given(customOneseoRepository.findAdmissionTickets()).willReturn(expectedTickets);
        }

        @Test
        @DisplayName("수험표 리스트를 반환한다")
        void it_return_admission_tickets() {
            List<AdmissionTicketsResDto> result = queryAdmissionTicketsService.execute();

            assertEquals(expectedTickets, result);
        }
    }
}
