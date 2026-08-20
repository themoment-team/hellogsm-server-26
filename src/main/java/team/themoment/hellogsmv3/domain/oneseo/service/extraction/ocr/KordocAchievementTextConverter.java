package team.themoment.hellogsmv3.domain.oneseo.service.extraction.ocr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocBlock;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocParseResult;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocTableCell;
import team.themoment.hellogsmv3.domain.oneseo.dto.internal.kordoc.KordocTableRow;

/**
 * kordoc {@code --format json} 표 재구성 결과를
 * {@link team.themoment.hellogsmv3.domain.oneseo.service.extraction.MiddleSchoolRecordParser}가
 * 원래 읽던 줄 형식으로 되돌립니다.
 *
 * <p>
 * 새로운 파서를 만들지 않고, 이미 실제 생활기록부로 검증된 기존 파서를 그대로 재사용하는 것이 목표입니다. 표가 아닌 블록(제목 등)에서
 * 학년 표시를 복원하고, 표 블록의 각 행에서 원점수/성취도 열(줄바꿈으로 분리됨)과 과목명 열(구분자 없이 붙어 있음)을 찾아 짝을
 * 맞춥니다. 어떤 열이 무엇인지 표에 명시되어 있지 않으므로, 각 열의 텍스트 모양으로 역할을 추정합니다.
 *
 * <p>
 * 짝을 맞추지 못한 행은 억지로 밀어 넣지 않고 건너뛰며, 그 과목명 원문을 {@code unrecognizedSubjectBlobs}로
 * 돌려주어 사용자 검수를 요청합니다. 표 모양을 추정하지 못한 행(출결 · 봉사 등)은 셀을 그대로 이어붙여 지나가는 줄로 남겨둡니다 —
 * 기존 파서의 정규식은 정확히 일치하는 줄만 소비하므로, 맞지 않는 줄이 섞여도 무시될 뿐 다른 결과를 해치지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class KordocAchievementTextConverter {

    private static final Pattern GRADE_MENTION = Pattern.compile("(\\d)?\\s*학년");
    private static final Pattern BARE_SEMESTER = Pattern.compile("^[12]$");
    // 원점수/과목평균 부분은 SUBJECT_ROW와 동일하게 선택 사항입니다. 예체능처럼 성취도만 있는 과목이 이 표에 섞여
    // 나올 가능성을 배제할 수 없어, 점수 없이 성취도 글자만 있는 줄도 원점수/성취도 열로 인식합니다.
    private static final Pattern SCORE_LINE = Pattern
            .compile("^(?:[\\d.,\\s]+/[\\d.,\\s]+\\s*(?:\\([^)]*\\))?\\s+)?[A-EP]\\s*(?:\\(\\s*\\d+\\s*\\))?$");
    private static final Pattern HANGUL = Pattern.compile("[가-힣]");

    private final KordocSubjectTokenizer subjectTokenizer;

    public KordocConversionResult convert(KordocParseResult parseResult) {
        List<String> lines = new ArrayList<>();
        List<String> unrecognizedSubjectBlobs = new ArrayList<>();

        for (KordocBlock block : parseResult.blocksOrEmpty()) {
            if (block.isTable()) {
                convertTable(block, lines, unrecognizedSubjectBlobs);
            } else {
                convertHeadingOrText(block, lines);
            }
        }

        return new KordocConversionResult(String.join("\n", lines), unrecognizedSubjectBlobs);
    }

    /** 표가 아닌 블록에서 학년 표시(예: "1학년")를 찾아 파서가 요구하는 {@code [N학년]} 줄로 되살립니다. */
    private void convertHeadingOrText(KordocBlock block, List<String> lines) {
        String text = block.textOrEmpty();
        Matcher matcher = GRADE_MENTION.matcher(text);
        if (!matcher.find()) {
            return;
        }
        String digit = matcher.group(1);
        lines.add("[" + (digit == null ? "" : digit) + "학년]");
    }

    private void convertTable(KordocBlock block, List<String> lines, List<String> unrecognizedSubjectBlobs) {
        for (KordocTableRow row : block.rowsOrEmpty()) {
            List<KordocTableCell> cells = row.cellsOrEmpty();
            Optional<KordocTableCell> scoreCell = findScoreCell(cells);
            if (scoreCell.isEmpty()) {
                convertPassthroughRow(cells, lines);
                continue;
            }

            List<String> scoreLines = scoreCell.get().textOrEmpty().lines().map(String::strip)
                    .filter(line -> !line.isEmpty()).toList();

            Optional<KordocTableCell> subjectCell = findSubjectCell(cells, scoreCell.get());
            Optional<List<String>> subjects = subjectCell
                    .flatMap(cell -> subjectTokenizer.tokenize(cell.textOrEmpty(), scoreLines.size()));

            if (subjects.isEmpty()) {
                subjectCell.ifPresent(cell -> unrecognizedSubjectBlobs.add(cell.textOrEmpty()));
                continue;
            }

            findSemesterDigit(cells).ifPresent(lines::add);
            for (int index = 0; index < subjects.get().size(); index++) {
                lines.add(subjects.get().get(index) + " " + scoreLines.get(index));
            }
        }
    }

    /** 원점수/성취도 열: 줄마다 "91/77.8 A(168)" 형태를 갖는 셀입니다. */
    private Optional<KordocTableCell> findScoreCell(List<KordocTableCell> cells) {
        return cells.stream().filter(cell -> {
            List<String> lines = cell.textOrEmpty().lines().map(String::strip).filter(line -> !line.isEmpty()).toList();
            return !lines.isEmpty() && lines.stream().allMatch(line -> SCORE_LINE.matcher(line).matches());
        }).findFirst();
    }

    /** 과목명 열: 원점수 셀도, 학기 숫자 하나짜리 셀도 아니면서 한글이 가장 많이 포함된 셀입니다. */
    private Optional<KordocTableCell> findSubjectCell(List<KordocTableCell> cells, KordocTableCell scoreCell) {
        return cells.stream().filter(cell -> cell != scoreCell)
                .filter(cell -> !BARE_SEMESTER.matcher(cell.textOrEmpty().strip()).matches())
                .max(Comparator.comparingInt(cell -> countHangul(cell.textOrEmpty())))
                .filter(cell -> countHangul(cell.textOrEmpty()) > 0);
    }

    private Optional<String> findSemesterDigit(List<KordocTableCell> cells) {
        return cells.stream().map(cell -> cell.textOrEmpty().strip())
                .filter(text -> BARE_SEMESTER.matcher(text).matches()).findFirst();
    }

    private int countHangul(String text) {
        Matcher matcher = HANGUL.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /** 원점수/성취도 열을 찾지 못한 행(출결 · 봉사 등으로 추정)은 셀을 이어붙여 그대로 지나가는 줄로 남깁니다. */
    private void convertPassthroughRow(List<KordocTableCell> cells, List<String> lines) {
        String joined = cells.stream().map(KordocTableCell::textOrEmpty).filter(text -> !text.isBlank())
                .reduce((a, b) -> a + " " + b).orElse("");
        if (!joined.isBlank()) {
            lines.add(joined);
        }
    }
}
