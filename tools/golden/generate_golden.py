#!/usr/bin/env python3
"""go-hellogsm-score-calculator(2026 시즌) 대비 golden test fixture 생성기.

Go 툴체인 없이 parity를 검증하기 위해, 2026 시즌 코드(커밋 4e8d668 이전,
졸업자 배점 36/36/54/54)의 big.Rat 산술을 fractions.Fraction으로 그대로 포팅해
기대값을 계산하고, Kotlin 테스트 소스(GoParityGoldenCases.kt)를 생성한다.

주의:
- Go의 RoundToThreeDecimals는 big.Rat → float64(×1000) → math.Round 경로지만,
  이 점수 범위(≤300)에서는 정확한 유리수 반올림(반올림 자리 5는 올림)과 동일하다.
- Go는 검정고시 평균을 JSON float64로 받으므로 이진 오차가 섞일 수 있다.
  fixture는 소수 둘째 자리 이하의 평균만 사용해 이 문제를 회피한다 (PLAN.md 7절 참고).
- 요강의 학기 몫 scale-5 중간 반올림은 Go에 없다. 생성 시 두 방식을 모두 계산해
  결과가 갈리는 케이스는 fixture에서 제외하고 표준 출력으로 보고한다.

실행: python3 tools/golden/generate_golden.py  (레포 루트에서)
"""

from fractions import Fraction as F
import random

OUT = "entrance-engine/src/test/kotlin/kr/hellogsm/entrance/engine/scoring/GoParityGoldenCases.kt"

CANDIDATE_WEIGHTS = {"1-2": 18, "2-1": 45, "2-2": 45, "3-1": 72}
GRADUATE_WEIGHTS = {"2-1": 36, "2-2": 36, "3-1": 54, "3-2": 54}


def round3(x: F) -> F:
    """Go RoundToThreeDecimals와 동일 (음수는 절대값 기준 반올림 = half away from zero)."""
    if x < 0:
        return -round3(-x)
    return F((x * 1000 + F(1, 2)).__floor__(), 1000)


def fmt3(x: F) -> str:
    n = (round3(x) * 1000).numerator // (round3(x) * 1000).denominator
    assert (round3(x) * 1000).denominator == 1
    n = int(round3(x) * 1000)
    return f"{n // 1000}.{n % 1000:03d}"


# ── Go 포팅 (2026 시즌, 커밋 4e8d668 이전) ──────────────────────────────

def go_semester_score(achievements, weight) -> F:
    if not achievements:
        return round3(F(0))
    total = sum(achievements)
    n = len([a for a in achievements if a != 0])
    return round3(F(total, n * 5) * weight)


def engine_semester_score(achievements, weight) -> F:
    """엔진(요강) 방식: 몫을 scale 5로 먼저 반올림한 뒤 배점을 곱한다."""
    total = sum(achievements)
    n = len(achievements)
    q = F(total, n * 5)
    q5 = F((q * 10**5 + F(1, 2)).__floor__(), 10**5)
    return round3(q5 * weight)


def fill_empty(sems: dict, gtype: str) -> dict:
    """Go BuildCalcDtoWithFillEmpty (2026 시즌 버전)."""
    s = {k: sems.get(k) for k in ["1-1", "1-2", "2-1", "2-2", "3-1", "3-2"]}
    if gtype == "GRADUATE" and s["3-2"] is None:
        s["3-2"] = s["3-1"]
    if s["3-1"] is None:
        s["3-1"] = s["3-2"]
    if s["2-1"] is None:
        s["2-1"] = s["2-2"]
    if s["2-2"] is None:
        s["2-2"] = s["2-1"]
    if gtype == "CANDIDATE" and s["1-2"] is None:
        s["1-2"] = s["1-1"] if s["1-1"] is not None else s["2-2"]
    return s


def go_arts(achievements) -> F:
    total = sum(achievements)
    n = len([a for a in achievements if a != 0])
    avg = round3(F(total, 5 * n))
    return round3(avg * 60)


def go_attendance(absences, lateness_counts) -> F:
    if sum(absences) >= 10:
        return round3(F(0))
    total = sum(absences) + sum(lateness_counts) // 3
    return round3(F(max(30 - 3 * total, 0)))


def go_volunteer(hours) -> F:
    def step(h):
        if h >= 7:
            return 10
        if h >= 6:
            return 8
        if h >= 5:
            return 6
        if h >= 4:
            return 4
        return 2

    return round3(F(sum(step(h) for h in hours)))


def go_transcript(case) -> dict:
    weights = CANDIDATE_WEIGHTS if case["type"] == "CANDIDATE" else GRADUATE_WEIGHTS
    filled = fill_empty(case["semesters"], case["type"])

    semesters, engine_semesters = {}, {}
    for slot, w in weights.items():
        semesters[slot] = go_semester_score(filled[slot], w)
        engine_semesters[slot] = engine_semester_score(filled[slot], w)

    general = round3(sum(semesters.values(), F(0)))
    arts = go_arts(case["arts"])
    subjects = round3(general + arts)
    attendance = go_attendance(
        [a[0] for a in case["attendance"]],
        [a[1] + a[2] + a[3] for a in case["attendance"]],
    )
    volunteer = go_volunteer(case["volunteer"])
    non_subjects = round3(attendance + volunteer)
    total = round3(subjects + non_subjects)

    diverged = [s for s in weights if semesters[s] != engine_semesters[s]]

    engine_general = round3(sum(engine_semesters.values(), F(0)))
    engine_subjects = round3(engine_general + arts)
    return {
        "semesters": semesters,
        "general": general,
        "arts": arts,
        "subjects": subjects,
        "attendance": attendance,
        "volunteer": volunteer,
        "total": total,
        "diverged": diverged,
        "engine": {
            "semesters": engine_semesters,
            "general": engine_general,
            "arts": arts,
            "subjects": engine_subjects,
            "attendance": attendance,
            "volunteer": volunteer,
            "total": round3(engine_subjects + non_subjects),
        },
    }


def go_ged(avg: F) -> dict:
    subjects = round3((avg - 50) / 50 * 240)
    if subjects < 0:
        subjects = F(0)
    volunteer = round3((avg - 40) / 60 * 30)
    if volunteer < 0:
        volunteer = F(0)
    attendance = F(30)
    non_subjects = round3(volunteer + attendance)
    total = round3(subjects + non_subjects)
    return {"subjects": subjects, "attendance": attendance, "volunteer": volunteer, "total": total}


# ── 케이스 생성 ─────────────────────────────────────────────────────────

rng = random.Random(20260720)


def random_achievements():
    return [rng.randint(1, 5) for _ in range(rng.randint(5, 12))]


def random_semesters(gtype: str) -> dict:
    """양쪽 구현의 결측 처리가 일치하는 패턴만 사용한다."""
    if gtype == "CANDIDATE":
        slots = ["1-2", "2-1", "2-2", "3-1"]
        pattern = rng.choice([[], [], [], ["1-2"], ["1-2", "+1-1"], ["2-1"], ["2-2"]])
    else:
        slots = ["2-1", "2-2", "3-1", "3-2"]
        pattern = rng.choice([[], [], [], ["3-2"], ["2-1"], ["2-2"]])

    sems = {}
    for slot in slots:
        if slot not in pattern:
            sems[slot] = random_achievements()
    if "+1-1" in pattern:  # 1-2 결측 + 1-1 제출 (같은 학년 다른 학기 대체)
        sems["1-1"] = random_achievements()
    return sems


def random_attendance():
    return [
        (rng.randint(0, 4), rng.randint(0, 4), rng.randint(0, 3), rng.randint(0, 3))
        for _ in range(3)
    ]


def make_cases():
    cases = []
    for i in range(40):
        cases.append({
            "name": f"CANDIDATE-{i + 1:03d}",
            "kind": "transcript",
            "type": "CANDIDATE",
            "semesters": random_semesters("CANDIDATE"),
            "arts": [rng.randint(3, 5) for _ in range(rng.randint(3, 9))],
            "attendance": random_attendance(),
            "volunteer": [rng.randint(0, 10) for _ in range(3)],
        })
    for i in range(40):
        cases.append({
            "name": f"GRADUATE-{i + 1:03d}",
            "kind": "transcript",
            "type": "GRADUATE",
            "semesters": random_semesters("GRADUATE"),
            "arts": [rng.randint(3, 5) for _ in range(rng.randint(3, 9))],
            "attendance": random_attendance(),
            "volunteer": [rng.randint(0, 10) for _ in range(3)],
        })

    # 경계값: 결석 10일, 환산 결석 경계, 봉사 계단 경계
    edge_attendances = [
        [(10, 0, 0, 0), (0, 0, 0, 0), (0, 0, 0, 0)],  # 결석 10일 → 0점
        [(9, 3, 0, 0), (0, 0, 0, 0), (0, 0, 0, 0)],   # 9일 + 지각 3회 = 10일 → 0점
        [(3, 2, 2, 1), (2, 1, 1, 0), (1, 0, 0, 2)],   # 결석 6 + ⌊9÷3⌋ = 9일 → 3점
        [(0, 2, 0, 0), (0, 2, 0, 0), (0, 2, 0, 0)],   # 지각 합산 6회 = 2일 (학년별 버림 아님)
    ]
    for i, att in enumerate(edge_attendances):
        cases.append({
            "name": f"EDGE-ATT-{i + 1:02d}",
            "kind": "transcript",
            "type": "CANDIDATE",
            "semesters": random_semesters("CANDIDATE"),
            "arts": [5, 5, 4, 4, 3, 3],
            "attendance": att,
            "volunteer": [7, 7, 7],
        })
    for i, vol in enumerate([[7, 6, 5], [4, 3, 0], [10, 0, 4], [3, 3, 3]]):
        cases.append({
            "name": f"EDGE-VOL-{i + 1:02d}",
            "kind": "transcript",
            "type": "GRADUATE",
            "semesters": random_semesters("GRADUATE"),
            "arts": [5, 4, 3],
            "attendance": random_attendance(),
            "volunteer": vol,
        })

    # 검정고시: 정수/소수 둘째 자리 평균 (float64 이진 오차 회피 — 모듈 docstring 참고)
    ged_avgs = ["100", "50", "49.99", "40", "60.5", "73.5", "88.88", "91.25", "77.77", "65.43"]
    ged_avgs += [str(rng.randint(50, 99)) + f".{rng.randint(0, 99):02d}" for _ in range(10)]
    for i, avg in enumerate(ged_avgs):
        cases.append({"name": f"GED-{i + 1:03d}", "kind": "ged", "avg": avg})
    return cases


# ── Kotlin 소스 생성 ────────────────────────────────────────────────────

def k_achievements(ints):
    return "ach(" + ", ".join(map(str, ints)) + ")"


def k_semester_ref(slot: str) -> str:
    y, s = slot.split("-")
    return f"SemesterRef({y}, {s})"


def emit_transcript_case(case, expected, comment: str = "") -> str:
    sem_entries = ",\n                ".join(
        f"{k_semester_ref(slot)} to {k_achievements(a)}" for slot, a in sorted(case["semesters"].items())
    )
    att_entries = ", ".join(
        f"{y + 1} to AttendanceRecord({a[0]}, {a[1]}, {a[2]}, {a[3]})" for y, a in enumerate(case["attendance"])
    )
    vol_entries = ", ".join(f"{y + 1} to {h}" for y, h in enumerate(case["volunteer"]))
    exp_sems = ", ".join(f'{k_semester_ref(s)} to "{fmt3(v)}"' for s, v in expected["semesters"].items())
    return f"""{comment}    GoldenCase(
        name = "{case['name']}",
        record = StudentRecord.Transcript(
            graduationType = GraduationType.{case['type']},
            generalAchievements = mapOf(
                {sem_entries},
            ),
            artsAchievements = {k_achievements(case['arts'])},
            attendanceByYear = mapOf({att_entries}),
            volunteerHoursByYear = mapOf({vol_entries}),
        ),
        expected = Expected(
            total = "{fmt3(expected['total'])}",
            subjects = "{fmt3(expected['subjects'])}",
            attendance = "{fmt3(expected['attendance'])}",
            volunteer = "{fmt3(expected['volunteer'])}",
            general = "{fmt3(expected['general'])}",
            arts = "{fmt3(expected['arts'])}",
            semesters = mapOf({exp_sems}),
        ),
    ),"""


def emit_ged_case(case, expected) -> str:
    return f"""    GoldenCase(
        name = "{case['name']}",
        record = StudentRecord.Ged(BigDecimal("{case['avg']}")),
        expected = Expected(
            total = "{fmt3(expected['total'])}",
            subjects = "{fmt3(expected['subjects'])}",
            attendance = "{fmt3(expected['attendance'])}",
            volunteer = "{fmt3(expected['volunteer'])}",
        ),
    ),"""


def main():
    entries, divergent_entries, diverged = [], [], []
    for case in make_cases():
        if case["kind"] == "ged":
            entries.append(emit_ged_case(case, go_ged(F(case["avg"]))))
        else:
            expected = go_transcript(case)
            if expected["diverged"]:
                diverged.append((case["name"], expected["diverged"]))
                diff = " / ".join(
                    f"{s}: Go {fmt3(expected['semesters'][s])} vs 요강 {fmt3(expected['engine']['semesters'][s])}"
                    for s in expected["diverged"]
                )
                comment = f"    // Go와 결과가 다른 학기 — {diff}\n"
                divergent_entries.append(emit_transcript_case(case, expected["engine"], comment))
                continue
            entries.append(emit_transcript_case(case, expected))

    body = "\n".join(entries)
    divergent_body = "\n".join(divergent_entries)
    source = f"""package kr.hellogsm.entrance.engine.scoring

// 이 파일은 tools/golden/generate_golden.py 가 생성한다. 손으로 수정하지 말 것.
// 기대값 출처: go-hellogsm-score-calculator 2026 시즌 코드(커밋 4e8d668 이전)의 big.Rat 산술 포팅.

import kr.hellogsm.entrance.plan.Achievement
import kr.hellogsm.entrance.plan.GraduationType
import kr.hellogsm.entrance.plan.SemesterRef
import java.math.BigDecimal

internal data class GoldenCase(
    val name: String,
    val record: StudentRecord,
    val expected: Expected,
)

internal data class Expected(
    val total: String,
    val subjects: String,
    val attendance: String,
    val volunteer: String,
    val general: String? = null,
    val arts: String? = null,
    val semesters: Map<SemesterRef, String>? = null,
)

/** 성취도 환산점수(5=A … 1=E) → Achievement */
private fun ach(vararg points: Int): List<Achievement> = points.map {{
    when (it) {{
        5 -> Achievement.A
        4 -> Achievement.B
        3 -> Achievement.C
        2 -> Achievement.D
        1 -> Achievement.E
        else -> error("유효하지 않은 성취도 환산점수: $it")
    }}
}}

/** Go 계산기와 엔진의 결과가 일치하는 케이스 — Go 산출값을 그대로 고정 */
internal val goParityGoldenCases: List<GoldenCase> = listOf(
{body}
)

/**
 * 요강의 학기 몫 scale-5 중간 반올림 때문에 Go(중간 반올림 없음)와 결과가 ±0.001 갈리는 케이스.
 * 요강이 정답 기준이므로(PLAN.md 7절 2항) 기대값은 요강 방식이다. Go 값은 각 케이스 주석 참고.
 */
internal val specDivergenceCases: List<GoldenCase> = listOf(
{divergent_body}
)
"""
    with open(OUT, "w") as f:
        f.write(source)

    print(f"생성 완료: {OUT} ({len(entries)}건)")
    if diverged:
        print("⚠️ 요강(scale-5 중간 반올림)과 Go(중간 반올림 없음)의 결과가 갈려 제외된 케이스:")
        for name, slots in diverged:
            print(f"  - {name}: {slots}")


if __name__ == "__main__":
    main()
