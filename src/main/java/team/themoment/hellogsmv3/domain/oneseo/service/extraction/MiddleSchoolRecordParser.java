package team.themoment.hellogsmv3.domain.oneseo.service.extraction;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.ExtractedTextDto;
import team.themoment.hellogsmv3.domain.oneseo.dto.response.*;
import team.themoment.hellogsmv3.domain.oneseo.entity.type.GraduationType;
import team.themoment.hellogsmv3.domain.oneseo.service.extraction.SubjectNameNormalizer.NormalizedSubject;

/**
 * 생활기록부 원문 텍스트를 원서 폼의 성적 구조로 변환합니다.
 *
 * <p>
 * 이 클래스는 텍스트를 어떤 방식으로 얻었는지에 의존하지 않습니다. PDF 텍스트 레이어에서 왔든 OCR에서 왔든 동일하게 동작하므로, 추출
 * 엔진을 교체해도 재사용됩니다.
 *
 * <p>
 * <b>배열 인덱싱 규칙 (프론트엔드 계약):</b>
 * <ul>
 * <li>일반교과 — {@code achievementX_Y[i]}는 {@code [표준 과목 9개..., 추가 과목...]}의 i번째
 * 과목에 대응합니다.</li>
 * <li>예체능 — {@code artsPhysicalAchievement[학기순번 * 3 + 과목순번]}이며, 과목 순서는 체육/음악/미술
 * 고정입니다. 학기 목록은 졸업 구분과 자유학기제 여부에 따라 달라집니다.</li>
 * </ul>
 *
 * <p>
 * <b>중요:</b> 아래 정규식들은 실제 생활기록부 샘플 없이 작성한 1차 안입니다. 샘플 PDF를 확보한 뒤 반드시 조정해야 하며,
 * 조정이 필요한 부분은 이 클래스의 상수 영역에 모아두었습니다.
 */
@Component
@RequiredArgsConstructor
public class MiddleSchoolRecordParser {

    private final SubjectNameNormalizer subjectNameNormalizer;

    // ---------------------------------------------------------------
    // 실제 샘플 확보 후 조정이 필요한 영역
    // ---------------------------------------------------------------

    /**
     * 학년 구분 표시. 예: {@code [1학년]}
     *
     * <p>
     * OCR이 숫자를 놓쳐 {@code [학년]}으로만 읽히는 경우가 실제로 확인되어 숫자를 선택 항목으로 두었습니다. 숫자가 없으면 직전
     * 학년의 다음 학년으로 간주합니다. 대괄호를 필수로 요구해서 {@code 학년 | 수업일수} 같은 표 머리글과 구분합니다.
     */
    private static final Pattern GRADE_MARKER = Pattern.compile("^\\[\\s*(\\d)?\\s*학년\\s*]");

    /**
     * 교과 성적 행.
     *
     * <p>
     * 실제 생활기록부 OCR 결과 기준입니다.
     *
     * <pre>
     * 1     국어            국어             91/77.8      A(168)
     * 1     예술(음악/미술)  음악                          A
     * 2     i   "" "9 |기술ㆍ가정           98/64.6      A(165)
     * </pre>
     *
     * <p>
     * 교과 열은 OCR이 심하게 뭉개지지만 과목 열과 성취도는 대체로 읽힙니다. 그래서 행 전체 구조를 맞추려 하지 않고, 줄 끝의 성취도와 그
     * 앞의 마지막 한글 덩어리만 잡습니다. 앞쪽 잡음이 어떻게 깨지든 영향을 받지 않습니다.
     *
     * <p>
     * 원점수는 {@code 91/77.8} 형태로 표준편차 괄호가 없고, OCR이 {@code 100/79. 8}처럼 소수점 뒤에 공백을 넣기도
     * 해서 숫자 안의 공백을 허용합니다.
     *
     * <p>
     * group1=학기, group2=과목, group3=성취도
     */
    private static final Pattern SUBJECT_ROW = Pattern.compile(
            "^([12])\\s+.*(?<![가-힣ㆍ·])([가-힣][가-힣ㆍ·]*)\\s+(?:[\\d.\\s]+/[\\d.\\s]+\\s*(?:\\([^)]*\\))?\\s+)?([A-E])\\s*(?:\\(\\s*\\d+\\s*\\))?\\s*$");

    /**
     * 출결 행. {@code 학년 수업일수 결석(질병/미인정/기타) 지각(3) 조퇴(3) 결과(3)} = 학년 + 수업일수 + 숫자 12개
     *
     * <p>
     * group1=학년, group2=수업일수, group3=숫자 12개
     */
    private static final Pattern ATTENDANCE_ROW = Pattern.compile("^([123])\\s+(\\d+)((?:\\s+\\d+){12})\\s*$");

    /** 봉사활동 행. 예: {@code 1 봉사활동 7} */
    private static final Pattern VOLUNTEER_ROW = Pattern.compile("^([123])\\s+봉사활동\\s+(\\d+)");

    /** 성취도 문자를 성취점수로 변환합니다. 예체능은 A~C(5~3)만 유효합니다. */
    private static final Map<Character, Integer> ACHIEVEMENT_SCORE = Map.of('A', 5, 'B', 4, 'C', 3, 'D', 2, 'E', 1);

    // ---------------------------------------------------------------

    public static final String FREE_YEAR_SYSTEM = "자유학년제";
    public static final String FREE_SEMESTER_SYSTEM = "자유학기제";

    /** 졸업자가 입력하는 학기. 프론트엔드 {@code artPhysicalGraduationArray}와 일치합니다. */
    private static final List<String> GRADUATE_SEMESTERS = List.of("2-1", "2-2", "3-1", "3-2");

    /** 자유학년제 졸업예정자가 입력하는 학기. 1학년 전체가 제외됩니다. */
    private static final List<String> CANDIDATE_FREE_YEAR_SEMESTERS = List.of("2-1", "2-2", "3-1");

    /** 자유학기제 졸업예정자가 입력하는 학기. */
    private static final List<String> CANDIDATE_FREE_SEMESTER_SEMESTERS = List.of("1-1", "1-2", "2-1", "2-2", "3-1");

    /** 성적 미이수 또는 해당 없음 */
    private static final int NOT_TAKEN = 0;

    /**
     * @param extractedText
     *            추출된 원문
     * @param graduationType
     *            졸업 구분
     * @param liberalSystem
     *            자유학기제 또는 자유학년제. 예체능 배열의 길이를 결정하므로 졸업예정자에게는 필수입니다.
     */
    public ExtractedAchievementResDto parse(ExtractedTextDto extractedText,
            GraduationType graduationType,
            String liberalSystem) {
        ParseContext context = new ParseContext();
        readLines(extractedText.rawText(), context);

        List<String> targetSemesters = resolveSemesters(graduationType, liberalSystem);
        List<String> newSubjects = List.copyOf(context.newSubjectOrder);
        List<String> orderedSubjects = new ArrayList<>(SubjectNameNormalizer.STANDARD_GENERAL_SUBJECTS);
        orderedSubjects.addAll(newSubjects);

        List<String> missingSemesters = targetSemesters.stream()
                .filter(semester -> !context.generalBySemester.containsKey(semester)).toList();
        missingSemesters.forEach(semester -> context.warnings.add(ExtractionWarningResDto.builder()
                .field(toFieldName(semester)).type(ExtractionWarningType.MISSING_SEMESTER)
                .message(semester + " 학기 성적을 찾지 못했습니다. 직접 입력해주세요.").build()));

        MiddleSchoolAchievementResDto achievement = MiddleSchoolAchievementResDto.builder()
                .achievement1_1(toScores("1-1", orderedSubjects, context))
                .achievement1_2(toScores("1-2", orderedSubjects, context))
                .achievement2_1(toScores("2-1", orderedSubjects, context))
                .achievement2_2(toScores("2-2", orderedSubjects, context))
                .achievement3_1(toScores("3-1", orderedSubjects, context))
                .achievement3_2(toScores("3-2", orderedSubjects, context))
                .generalSubjects(SubjectNameNormalizer.STANDARD_GENERAL_SUBJECTS).newSubjects(newSubjects)
                .artsPhysicalSubjects(SubjectNameNormalizer.STANDARD_ARTS_PHYSICAL_SUBJECTS)
                .artsPhysicalAchievement(toArtsPhysicalScores(targetSemesters, context))
                .absentDays(toList(context.absentDays)).attendanceDays(toList(context.attendanceDays))
                .volunteerTime(toList(context.volunteerTime)).liberalSystem(liberalSystem).build();

        warnMissingAttendanceAndVolunteer(context);

        ExtractionMetaResDto meta = ExtractionMetaResDto.builder()
                .confidence(calculateConfidence(context, targetSemesters, extractedText.recognitionConfidence()))
                .hasTextLayer(extractedText.hasTextLayer()).source(extractedText.source())
                .pageCount(extractedText.pageCount()).unrecognizedSubjects(List.copyOf(context.unrecognizedSubjects))
                .missingSemesters(missingSemesters).warnings(List.copyOf(context.warnings)).build();

        return ExtractedAchievementResDto.builder().achievement(achievement).meta(meta).build();
    }

    /**
     * 원서 폼이 입력받는 학기 목록을 반환합니다.
     *
     * <p>
     * 프론트엔드의 {@code artPhysicalGraduationArray} 등과 일치해야 합니다.
     */
    private List<String> resolveSemesters(GraduationType graduationType, String liberalSystem) {
        if (graduationType == GraduationType.GRADUATE) {
            return GRADUATE_SEMESTERS;
        }
        return FREE_SEMESTER_SYSTEM.equals(liberalSystem)
                ? CANDIDATE_FREE_SEMESTER_SEMESTERS
                : CANDIDATE_FREE_YEAR_SEMESTERS;
    }

    private void readLines(String rawText, ParseContext context) {
        String[] lines = rawText.split("\\R");
        List<Integer> markerGrades = resolveMarkerGrades(lines);

        int currentGrade = 0;
        int markerIndex = 0;

        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty()) {
                continue;
            }

            if (GRADE_MARKER.matcher(line).find()) {
                currentGrade = markerGrades.get(markerIndex++);
            }

            if (readAttendance(line, context) || readVolunteer(line, context)) {
                continue;
            }
            if (currentGrade != 0) {
                readSubject(line, currentGrade, context);
            }
        }
    }

    /**
     * 학년 구분 표시들의 학년을 결정합니다.
     *
     * <p>
     * OCR이 숫자를 놓쳐 {@code [학년]}으로만 읽히는 경우가 있고, 페이지를 선별해서 읽으면 앞쪽 표시가 통째로 빠지기도 합니다. 두
     * 경우 모두 앞에서부터 세는 방식으로는 학년이 밀립니다.
     *
     * <p>
     * 학년 구분 표시는 문서에서 반드시 오름차순으로 나오므로, <b>숫자가 읽힌 표시 하나만 있으면 나머지를 상대 위치로 역산</b>할 수
     * 있습니다. 예를 들어 {@code [학년] ... [3학년]}이면 앞의 것은 2학년입니다.
     *
     * <p>
     * 숫자가 하나도 읽히지 않았다면 1학년부터 순서대로 매깁니다.
     */
    private List<Integer> resolveMarkerGrades(String[] lines) {
        List<Integer> markers = new ArrayList<>();
        for (String rawLine : lines) {
            Matcher matcher = GRADE_MARKER.matcher(rawLine.strip());
            if (matcher.find()) {
                markers.add(toGradeOrNull(matcher.group(1)));
            }
        }

        int anchorIndex = -1;
        for (int index = 0; index < markers.size(); index++) {
            if (markers.get(index) != null) {
                anchorIndex = index;
                break;
            }
        }

        // 읽힌 숫자가 하나도 없으면 마지막 구간을 3학년으로 보고 거슬러 올라갑니다.
        // 교과 성적은 3학년까지 이어지므로 앞에서부터 1학년으로 세는 것보다 맞을 확률이 높습니다.
        int anchorGrade = anchorIndex < 0 ? 3 : markers.get(anchorIndex);
        int anchorPosition = anchorIndex < 0 ? markers.size() - 1 : anchorIndex;

        List<Integer> resolved = new ArrayList<>(markers.size());
        for (int index = 0; index < markers.size(); index++) {
            resolved.add(Math.clamp(anchorGrade + (index - anchorPosition), 1, 3));
        }
        return resolved;
    }

    /**
     * 학년 구분 표시에서 읽힌 숫자를 학년으로 바꿉니다.
     *
     * <p>
     * OCR이 {@code [2학년]}을 {@code [9학년]}으로, {@code [3학년]}을 {@code [8학년]}으로 읽는 사례가
     * 실제로 확인되었습니다. 중학교에 없는 학년이 나오면 잘못 읽은 것으로 보고 모른다고 처리해, 다른 표시를 기준으로 역산하게 둡니다.
     */
    private Integer toGradeOrNull(String rawDigit) {
        if (rawDigit == null) {
            return null;
        }
        int grade = Integer.parseInt(rawDigit);
        return grade >= 1 && grade <= 3 ? grade : null;
    }

    private void readSubject(String line, int grade, ParseContext context) {
        Matcher matcher = SUBJECT_ROW.matcher(line);
        if (!matcher.find()) {
            return;
        }

        int semester = Integer.parseInt(matcher.group(1));
        String rawSubject = matcher.group(2);
        char letter = matcher.group(3).charAt(0);

        NormalizedSubject subject = subjectNameNormalizer.normalize(rawSubject);
        int score = ACHIEVEMENT_SCORE.getOrDefault(letter, NOT_TAKEN);
        String semesterKey = grade + "-" + semester;

        if (subjectNameNormalizer.isArtsPhysical(subject.normalized())) {
            readArtsPhysical(semesterKey, subject, score, letter, context);
            return;
        }

        if (subject.matchType() == SubjectNameNormalizer.MatchType.UNKNOWN) {
            context.unrecognizedSubjects.add(subject.raw());
            context.newSubjectOrder.add(subject.normalized());
        }
        context.generalBySemester.computeIfAbsent(semesterKey, key -> new LinkedHashMap<>()).put(subject.normalized(),
                score);

        if (subject.needsReview()) {
            context.warnings.add(ExtractionWarningResDto.builder().field("generalSubjects").rawText(subject.raw())
                    .type(subject.matchType() == SubjectNameNormalizer.MatchType.UNKNOWN
                            ? ExtractionWarningType.UNRECOGNIZED_SUBJECT
                            : ExtractionWarningType.LOW_CONFIDENCE)
                    .message("'" + subject.raw() + "' 과목을 확인해주세요.").build());
        }
    }

    private void readArtsPhysical(String semesterKey,
            NormalizedSubject subject,
            int score,
            char letter,
            ParseContext context) {
        if (score != NOT_TAKEN && score < 3) {
            context.warnings.add(ExtractionWarningResDto.builder().field("artsPhysicalAchievement")
                    .rawText(String.valueOf(letter)).type(ExtractionWarningType.OUT_OF_RANGE)
                    .message("예체능 성취도 '" + letter + "'는 원서에 입력할 수 없는 값입니다. 직접 확인해주세요.").build());
            score = NOT_TAKEN;
        }
        context.artsPhysicalBySemester.computeIfAbsent(semesterKey, key -> new LinkedHashMap<>())
                .put(subject.normalized(), score);
    }

    private boolean readAttendance(String line, ParseContext context) {
        Matcher matcher = ATTENDANCE_ROW.matcher(line);
        if (!matcher.find()) {
            return false;
        }

        int grade = Integer.parseInt(matcher.group(1));
        int[] values = Arrays.stream(matcher.group(3).strip().split("\\s+")).mapToInt(Integer::parseInt).toArray();

        // 결석 질병/미인정/기타 합산
        context.absentDays[grade - 1] = values[0] + values[1] + values[2];
        // 학년별 지각, 조퇴, 결과 순으로 3칸씩 배치
        context.attendanceDays[(grade - 1) * 3] = values[3] + values[4] + values[5];
        context.attendanceDays[(grade - 1) * 3 + 1] = values[6] + values[7] + values[8];
        context.attendanceDays[(grade - 1) * 3 + 2] = values[9] + values[10] + values[11];
        return true;
    }

    private boolean readVolunteer(String line, ParseContext context) {
        Matcher matcher = VOLUNTEER_ROW.matcher(line);
        if (!matcher.find()) {
            return false;
        }
        int grade = Integer.parseInt(matcher.group(1));
        context.volunteerTime[grade - 1] = Integer.parseInt(matcher.group(2));
        return true;
    }

    private List<Integer> toScores(String semesterKey, List<String> orderedSubjects, ParseContext context) {
        Map<String, Integer> scores = context.generalBySemester.get(semesterKey);
        if (scores == null) {
            return null;
        }

        List<Integer> result = new ArrayList<>(orderedSubjects.size());
        for (int index = 0; index < orderedSubjects.size(); index++) {
            String subject = orderedSubjects.get(index);
            Integer score = scores.get(subject);
            if (score == null) {
                context.warnings.add(ExtractionWarningResDto.builder().field(toFieldName(semesterKey)).index(index)
                        .rawText(subject).type(ExtractionWarningType.MISSING_SUBJECT)
                        .message(semesterKey + " 학기의 '" + subject + "' 성취도를 찾지 못했습니다.").build());
            }
            result.add(score == null ? NOT_TAKEN : score);
        }
        return result;
    }

    /**
     * 예체능 성취점수를 학기 우선 순서로 평탄화합니다. 인덱스는 {@code 학기순번 * 3 + 과목순번}이며 과목 순서는 체육/음악/미술
     * 고정입니다.
     *
     * <p>
     * 프론트엔드의 {@code artPhysicalGraduationIndexArray}(체육 [0,3,6,9] / 음악 [1,4,7,10] /
     * 미술 [2,5,8,11])와 동일한 규칙입니다.
     */
    private List<Integer> toArtsPhysicalScores(List<String> targetSemesters, ParseContext context) {
        List<Integer> result = new ArrayList<>(targetSemesters.size() * 3);
        for (String semester : targetSemesters) {
            Map<String, Integer> scores = context.artsPhysicalBySemester.getOrDefault(semester, Map.of());
            for (String subject : SubjectNameNormalizer.STANDARD_ARTS_PHYSICAL_SUBJECTS) {
                result.add(scores.getOrDefault(subject, NOT_TAKEN));
            }
        }
        return result;
    }

    /** 학기 키를 응답 필드명으로 변환합니다. 예: 2-1 → achievement2_1 */
    private String toFieldName(String semesterKey) {
        return "achievement" + semesterKey.replace('-', '_');
    }

    /** {@code Stream.toList()}는 null 요소를 허용하므로, 읽지 못한 칸이 null인 채로 응답에 실립니다. */
    private List<Integer> toList(Integer[] values) {
        return Arrays.stream(values).toList();
    }

    /**
     * 읽지 못한 출결 · 봉사 항목에 검수 경고를 붙입니다.
     *
     * <p>
     * 성적은 못 읽으면 null이 되어 빈칸으로 보이는데 출결 · 봉사만 0으로 채워지고 있었습니다. 0은 "결석 없음 · 봉사 0시간"이라는
     * 멀쩡한 값으로 보여서, 지원자가 인식 실패를 눈치채지 못하고 그대로 제출할 위험이 있습니다.
     *
     * <p>
     * 출결은 학년마다 한 행에서 결석 · 지각 · 조퇴 · 결과를 한꺼번에 읽으므로, 실패해도 학년당 경고 하나만 남깁니다. 개별 칸은 값이
     * null인 것으로 구분할 수 있습니다.
     */
    private void warnMissingAttendanceAndVolunteer(ParseContext context) {
        for (int grade = 1; grade <= 3; grade++) {
            if (context.absentDays[grade - 1] == null) {
                context.warnings.add(ExtractionWarningResDto.builder().field("absentDays").index(grade - 1)
                        .type(ExtractionWarningType.MISSING_ATTENDANCE)
                        .message(grade + "학년 출결 정보를 찾지 못했습니다. 직접 입력해주세요.").build());
            }
            if (context.volunteerTime[grade - 1] == null) {
                context.warnings.add(ExtractionWarningResDto.builder().field("volunteerTime").index(grade - 1)
                        .type(ExtractionWarningType.MISSING_VOLUNTEER)
                        .message(grade + "학년 봉사활동 시간을 찾지 못했습니다. 직접 입력해주세요.").build());
            }
        }
    }

    /**
     * 추출 결과를 얼마나 믿을 수 있는지 0.00 ~ 1.00으로 계산합니다.
     *
     * <p>
     * 이전에는 경고 개수만으로 계산했는데, 인식 자체가 실패해 행을 통째로 놓치면 경고가 생기지 않아 <b>못 읽을수록 점수가 올라가는</b>
     * 문제가 있었습니다. 실제로 해상도를 낮춰 결과가 나빠졌을 때 점수가 0.44에서 0.56으로 올랐습니다.
     *
     * <p>
     * 그래서 서로 다른 세 신호를 곱합니다. 하나라도 나쁘면 전체가 내려갑니다.
     *
     * <ul>
     * <li><b>인식 신뢰도</b> — 엔진이 글자를 얼마나 확신했는지. 글자가 뭉개지면 떨어지므로 그럴듯하지만 틀린 결과를
     * 걸러냅니다.</li>
     * <li><b>학기 충족도</b> — 필요한 학기 중 성적을 찾은 비율. 통째로 놓친 학기가 여기서 잡힙니다.</li>
     * <li><b>과목 충족도</b> — 학기마다 과목 수가 고르게 나왔는지. 한 학기만 유독 적으면 그 학기를 제대로 못 읽었다는
     * 뜻입니다.</li>
     * </ul>
     */
    private BigDecimal calculateConfidence(ParseContext context,
            List<String> targetSemesters,
            double recognitionConfidence) {
        if (context.generalBySemester.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }

        List<Integer> foundCounts = targetSemesters.stream()
                .map(semester -> context.generalBySemester.getOrDefault(semester, Map.of()).size()).toList();

        long semestersWithData = foundCounts.stream().filter(count -> count > 0).count();
        double semesterCoverage = (double) semestersWithData / targetSemesters.size();

        // 학생이 실제로 듣는 과목 수는 알 수 없으므로, 가장 많이 읽힌 학기를 기준으로 삼습니다.
        int bestCount = foundCounts.stream().mapToInt(Integer::intValue).max().orElse(0);
        double subjectCoverage = bestCount == 0
                ? 0
                : (double) foundCounts.stream().mapToInt(Integer::intValue).sum()
                        / (bestCount * targetSemesters.size());

        double confidence = recognitionConfidence * semesterCoverage * subjectCoverage;
        return BigDecimal.valueOf(Math.clamp(confidence, 0, 1)).setScale(2, RoundingMode.HALF_UP);
    }

    /** 한 번의 파싱 동안 누적되는 상태입니다. */
    private static final class ParseContext {
        private final Map<String, Map<String, Integer>> generalBySemester = new LinkedHashMap<>();
        private final Map<String, Map<String, Integer>> artsPhysicalBySemester = new LinkedHashMap<>();
        private final Set<String> newSubjectOrder = new LinkedHashSet<>();
        private final Set<String> unrecognizedSubjects = new LinkedHashSet<>();
        private final List<ExtractionWarningResDto> warnings = new ArrayList<>();
        // 읽지 못한 칸은 null로 남깁니다. 0으로 채우면 "개근"으로 읽혀 지원자가 그대로 제출할 수 있습니다.
        private final Integer[] absentDays = new Integer[3];
        private final Integer[] attendanceDays = new Integer[9];
        private final Integer[] volunteerTime = new Integer[3];
    }
}
