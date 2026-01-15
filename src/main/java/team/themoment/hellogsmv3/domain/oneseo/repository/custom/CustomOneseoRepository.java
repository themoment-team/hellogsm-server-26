package team.themoment.hellogsmv3.domain.oneseo.repository.custom;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import team.themoment.hellogsmv3.domain.oneseo.dto.internal.FoundMemberAndOneseoDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.TestResultTag;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.AdmissionTicketsResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseoResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.EntranceTestResult;
import team.themoment.hellogsmv3.domain.oneseo.entity.Oneseo;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.ScreeningCategory;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;

public interface CustomOneseoRepository {

    /**
     * 같은 전형의 가장 높은 submitCode를 반환합니다.
     * Screening.EXTRA_VETERANS,Screening.EXTRA_ADMISSION은 같은 전형으로 간주합니다.
     */
    Integer findMaxSubmitCodeByScreening(ScreeningCategory screeningCategory);

    Page<SearchOneseoResDto> findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(String keyword,
            ScreeningCategory screening,
            YesNo isSubmitted,
            TestResultTag testResultTag,
            Pageable pageable);

    List<AdmissionTicketsResDto> findAdmissionTickets();

    Optional<Oneseo> findByGuardianOrTeacherPhoneNumberAndSubmitCode(String phoneNumber, String submitCode);

    Optional<Oneseo> findByGuardianOrTeacherPhoneNumberAndExaminationNumber(String phoneNumber,
            String examinationNumber);

    List<Oneseo> findAllByScreeningWithAllDetails(Screening screening);

    List<Oneseo> findAllFailedWithAllDetails();

    Map<String, EntranceTestResult> findEntranceTestResultByExaminationNumbersIn(List<String> examinationNumbers);

    FoundMemberAndOneseoDto findMemberAndOneseoByMemberId(Long memberId);
}
