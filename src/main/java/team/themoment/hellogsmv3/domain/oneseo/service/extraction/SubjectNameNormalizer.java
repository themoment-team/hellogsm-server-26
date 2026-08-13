package team.themoment.hellogsmv3.domain.oneseo.service.extraction;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * 생활기록부에 적힌 과목명을 원서 폼의 표준 과목명으로 정규화합니다.
 *
 * <p>
 * 학교마다 같은 과목을 다르게 표기합니다. 예를 들어 {@code 사회(역사포함)}는 표준 과목 {@code 사회}와 같은 과목이지만
 * 문자열이 다릅니다. 이 문제는 텍스트를 어떤 방식으로 읽었는지와 무관하게 발생하므로, 추출 엔진을 바꾸어도 사라지지 않습니다.
 *
 * <p>
 * 매칭에 실패해도 예외를 던지지 않습니다. 표준 과목이 아니면 추가 과목({@code newSubjects})으로 넘기고 사용자에게 검수를
 * 요청합니다.
 */
@Component
public class SubjectNameNormalizer {

    /**
     * 원서 폼의 표준 일반교과 과목명입니다.
     *
     * <p>
     * 프론트엔드 {@code packages/constants/src/subject.ts}의 {@code GENERAL_SUBJECTS}와
     * <b>순서까지 정확히 일치해야 합니다.</b> 성취점수 배열이 이 순서로 인덱싱되기 때문입니다.
     */
    public static final List<String> STANDARD_GENERAL_SUBJECTS = List
            .of("국어", "사회", "도덕", "역사", "수학", "과학", "기술가정", "정보", "영어");

    /**
     * 원서 폼의 표준 예체능 과목명입니다.
     *
     * <p>
     * 프론트엔드의 {@code ARTS_PHYSICAL_SUBJECTS}와 순서까지 일치해야 합니다.
     */
    public static final List<String> STANDARD_ARTS_PHYSICAL_SUBJECTS = List.of("체육", "음악", "미술");

    /**
     * 표기 차이 사전.
     *
     * <p>
     * 실제 생활기록부 샘플을 확인하면서 계속 추가해야 하는 부분입니다.
     */
    private static final Map<String, String> ALIAS = Map.ofEntries(Map.entry("사회역사포함", "사회"),
            Map.entry("사회역사", "사회"),
            Map.entry("역사사회포함", "역사"),
            Map.entry("기술가정", "기술가정"),
            Map.entry("기술가정정보", "기술가정"),
            Map.entry("생활국어", "국어"),
            Map.entry("생활영어", "영어"),
            Map.entry("생활수학", "수학"),
            Map.entry("사회역사포함도덕", "사회"));

    private static final Set<String> ALL_STANDARD = Set.copyOf(concat());

    private static List<String> concat() {
        return java.util.stream.Stream
                .concat(STANDARD_GENERAL_SUBJECTS.stream(), STANDARD_ARTS_PHYSICAL_SUBJECTS.stream()).toList();
    }

    /**
     * @param rawSubject
     *            생활기록부에 적힌 원문 과목명
     * @return 정규화 결과
     */
    public NormalizedSubject normalize(String rawSubject) {
        String stripped = stripDecorations(rawSubject);

        if (ALL_STANDARD.contains(stripped)) {
            return new NormalizedSubject(stripped, rawSubject, MatchType.EXACT);
        }

        String alias = ALIAS.get(stripped);
        if (alias != null) {
            return new NormalizedSubject(alias, rawSubject, MatchType.ALIAS);
        }

        String fuzzy = findWithinEditDistanceOne(stripped);
        if (fuzzy != null) {
            return new NormalizedSubject(fuzzy, rawSubject, MatchType.FUZZY);
        }

        return new NormalizedSubject(rawSubject, rawSubject, MatchType.UNKNOWN);
    }

    /** 표준 예체능 과목인지 여부 */
    public boolean isArtsPhysical(String normalizedSubject) {
        return STANDARD_ARTS_PHYSICAL_SUBJECTS.contains(normalizedSubject);
    }

    /** 공백, 괄호와 그 내용, 중점 등 표기 장식을 제거합니다. */
    private String stripDecorations(String rawSubject) {
        if (rawSubject == null) {
            return "";
        }
        return rawSubject.replaceAll("\\([^)]*\\)", "").replaceAll("[\\s·・ㆍ/,~-]", "").strip();
    }

    /** 오타 한 글자 수준의 차이는 표준 과목으로 보정합니다. 보정된 값은 사용자 검수 대상으로 표시됩니다. */
    private String findWithinEditDistanceOne(String candidate) {
        if (candidate.length() < 2) {
            return null;
        }
        for (String standard : ALL_STANDARD) {
            if (editDistance(candidate, standard) <= 1) {
                return standard;
            }
        }
        return null;
    }

    private int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    /**
     * @param normalized
     *            표준 과목명. 매칭 실패 시 원문 그대로입니다.
     * @param raw
     *            생활기록부 원문
     * @param matchType
     *            매칭 방식
     */
    public record NormalizedSubject(String normalized, String raw, MatchType matchType) {

        /** 사용자 검수가 필요한지 여부 */
        public boolean needsReview() {
            return matchType == MatchType.FUZZY || matchType == MatchType.UNKNOWN;
        }
    }

    public enum MatchType {
        /** 표준 과목명과 정확히 일치 */
        EXACT,
        /** 표기 차이 사전으로 매칭 */
        ALIAS,
        /** 편집 거리 1 이내로 보정. 검수 필요 */
        FUZZY,
        /** 매칭 실패. 추가 과목으로 처리하고 검수 필요 */
        UNKNOWN
    }
}
