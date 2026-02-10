package team.themoment.hellogsmv3.domain.oneseo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.request.TestResultTag;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseoPageInfoDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseoResDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.SearchOneseosResDto;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.ScreeningCategory;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.YesNo;
import team.themoment.hellogsmv3.domain.oneseo.repository.OneseoRepository;

@Service
@RequiredArgsConstructor
public class SearchOneseoService {

    private final OneseoRepository oneseoRepository;

    @Transactional(readOnly = true)
    public SearchOneseosResDto execute(Integer page,
            Integer size,
            TestResultTag testResultTag,
            ScreeningCategory screeningTag,
            YesNo isSubmitted,
            String keyword) {

        Pageable pageable = PageRequest.of(page, size);
        Page<SearchOneseoResDto> oneseoPage = findOneseoByTagsAndKeyword(testResultTag,
                screeningTag,
                isSubmitted,
                keyword,
                pageable);

        SearchOneseoPageInfoDto infoDto = SearchOneseoPageInfoDto.builder().totalPages(oneseoPage.getTotalPages())
                .totalElements(oneseoPage.getTotalElements()).build();

        return SearchOneseosResDto.builder().info(infoDto).oneseos(oneseoPage.getContent()).build();
    }

    private Page<SearchOneseoResDto> findOneseoByTagsAndKeyword(TestResultTag testResultTag,
            ScreeningCategory screeningTag,
            YesNo isSubmitted,
            String keyword,
            Pageable pageable) {
        return oneseoRepository.findAllByKeywordAndScreeningAndSubmissionStatusAndTestResult(keyword,
                screeningTag,
                isSubmitted,
                testResultTag,
                pageable);
    }
}
