package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import team.themoment.hellogsmv3.domain.oneseo.service.extraction.SubjectNameNormalizer;

/**
 * kordoc이 표를 재구성할 때 과목명 열이 구분자 없이 붙어서 나오는 문제를 해결합니다. 예:
 * {@code "국어역사수학과학기술가정정보영어"} → {@code [국어, 역사, 수학, 과학, 기술가정, 정보, 영어]}.
 *
 * <p>
 * 표준 과목 9개({@link SubjectNameNormalizer#STANDARD_GENERAL_SUBJECTS})만으로 문자열 전체를
 * 빈틈없이 분해할 수 있을 때만 성공으로 봅니다. 선택 과목처럼 사전에 없는 과목명이 섞여 있으면 어디서 잘라야 할지 알 수 없으므로,
 * 억지로 나누지 않고 실패를 반환해 호출자가 검수 대상으로 표시하게 합니다.
 */
@Component
public class KordocSubjectTokenizer {

    private static final List<String> DICTIONARY_BY_LENGTH_DESC = SubjectNameNormalizer.STANDARD_GENERAL_SUBJECTS
            .stream().sorted(Comparator.comparingInt(String::length).reversed()).toList();

    /**
     * @param subjectBlob
     *            구분자 없이 붙어 있는 과목명 문자열
     * @param expectedCount
     *            같은 행의 원점수/성취도 셀을 줄바꿈으로 나눈 개수. 실제 과목 수의 근거입니다.
     * @return 분해된 과목명 목록. 표준 과목만으로 전체를 나누지 못했거나 개수가 맞지 않으면 빈 값입니다.
     */
    public Optional<List<String>> tokenize(String subjectBlob, int expectedCount) {
        String normalized = stripDecorations(subjectBlob);
        if (normalized.isEmpty() || expectedCount <= 0) {
            return Optional.empty();
        }

        List<String> tokens = segment(normalized, 0, new HashMap<>());
        if (tokens == null || tokens.size() != expectedCount) {
            return Optional.empty();
        }
        return Optional.of(tokens);
    }

    private String stripDecorations(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[\\s·・ㆍ/,~-]", "").strip();
    }

    /** 표준 과목 사전으로 문자열 전체를 빈틈없이 나누는 하나의 경로를 찾습니다. 매 위치에서 가장 긴 과목명을 먼저 시도합니다. */
    private List<String> segment(String text, int start, Map<Integer, List<String>> memo) {
        if (start == text.length()) {
            return new ArrayList<>();
        }
        if (memo.containsKey(start)) {
            return memo.get(start);
        }

        // 가장 긴 과목명부터 시도하되, 그 뒤가 막히면(rest == null) 되돌아가 다음 후보를 시도합니다(백트래킹).
        // 표준 과목 9개는 서로 접두사 관계가 없어 실제로는 되돌아갈 일이 없지만, 사전이 바뀌어도 안전하도록 남겨둡니다.
        for (String subject : DICTIONARY_BY_LENGTH_DESC) {
            if (text.startsWith(subject, start)) {
                List<String> rest = segment(text, start + subject.length(), memo);
                if (rest != null) {
                    List<String> result = new ArrayList<>();
                    result.add(subject);
                    result.addAll(rest);
                    memo.put(start, result);
                    return result;
                }
            }
        }

        memo.put(start, null);
        return null;
    }
}
