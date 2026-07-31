#!/usr/bin/env python3
"""go-hellogsm(전형 배치, 2026 시즌) 대비 golden test fixture 생성기.

2026 시즌 배치 로직을 그대로 포팅해 1차 평가 → 2차 평가 → 학과 배정의 기대값을 계산하고,
테스트 리소스(batch_parity.txt — BatchParityGoldenTest가 파싱)를 생성한다.

기준 코드: go-hellogsm 커밋 da09df4 이전 (2026 시즌 상수 — 1차 84/11/2/1, 2차 64/8/2/1,
학과 36/18/18, 정원외 학과당 2명).

주의:
- 최종 동점자 처리 순서는 요강(역량검사 → 심층면접 → 교과…)을 따른다. 2026 시즌에 실제
  돌았던 코드는 순서가 반대(교과 먼저)인 버그가 있었고 f4b17bc(2026-07-15)에서 수정됨 —
  요강이 정답이므로 fixture는 수정된 순서 기준이다.
- 2차 합성점수에 1차 총점 ÷ 3 × 0.5가 들어가므로, 1차 총점을 0.006의 배수로 생성해
  합성점수가 정확히 scale 3이 되게 한다 (반올림 차이가 fixture에 개입하지 않도록).
- 선발 경계·배정 순서의 완전 동점이 없도록 생성 후 검증한다 (있으면 결정 불능이므로).

실행: python3 tools/golden/generate_batch_golden.py  (레포 루트에서)
"""

import random
from fractions import Fraction as F

OUT = "entrance-engine/src/test/resources/golden/batch_parity.txt"

# 2026 시즌 상수 (go-hellogsm types/constant.go, da09df4 이전)
FIRST_LIMITS = {"EXT_SPECIAL": 1, "EXT_VETERANS": 2, "SPE": 11, "GEN": 84}
SECOND_LIMITS = {"EXT_SPECIAL": 1, "EXT_VETERANS": 2, "SPE": 8, "GEN": 64}
MAJOR_CAPACITY = {"SW": 36, "IOT": 18, "AI": 18}
EXTRA_CAP_PER_MAJOR = 2
INF = F(10 ** 12)  # NULL은 DESC 정렬에서 항상 뒤 (음수 점수가 없으므로)


class Applicant:
    def __init__(self, id, wanted, ged, subjects_ged, general, sems, arts, att, vol, comp, inter, choices):
        self.id = id
        self.wanted = wanted
        self.ged = ged
        self.subjects_ged = subjects_ged  # GED 교과 환산점수 (내신이면 None)
        self.general = general            # Fraction | None (GED)
        self.semesters = sems             # {"3-1": F|None, ...}
        self.arts = arts
        self.att = att
        self.vol = vol
        self.comp = comp                  # Fraction | None (미응시)
        self.inter = inter
        self.choices = choices

    @property
    def non_subjects(self):
        return self.att + self.vol

    @property
    def doc(self):
        """1차 총점 (교과 + 비교과)"""
        subjects = self.subjects_ged if self.ged else self.general + self.arts
        return subjects + self.non_subjects

    def common_tiebreak(self):
        def neg(v):
            return INF if v is None else -v

        # 일반교과 → 3-1 → 2-2 → 2-1 → 1-2 → 비교과 (GED는 교과·학기 값이 없어 뒤로 밀림)
        return (
            neg(self.general),
            neg(self.semesters["3-1"]), neg(self.semesters["2-2"]),
            neg(self.semesters["2-1"]), neg(self.semesters["1-2"]),
            neg(self.non_subjects),
        )

    def composite(self):
        return (self.doc / 3) * F(1, 2) + self.comp * F(3, 10) + self.inter * F(1, 5)


# ── 배치 파이프라인 포팅 ────────────────────────────────────────────────

def select(pool, limit, sort_key):
    """점수순 상위 limit명 선발. 경계 완전 동점은 fixture로 부적합하므로 실패시킨다."""
    ordered = sorted(pool, key=sort_key)
    if len(ordered) > limit > 0:
        assert sort_key(ordered[limit - 1]) != sort_key(ordered[limit]), \
            "선발 경계 완전 동점: %s vs %s" % (ordered[limit - 1].id, ordered[limit].id)
    return ordered[:limit], ordered[limit:]


def run_first(applicants):
    """1차 평가: 특례 → 보훈 → 특별(+정원외 탈락 편입) → 일반(+특별 탈락 편입)"""
    key = lambda a: (-a.doc,) + a.common_tiebreak()

    pools = {code: [a for a in applicants if a.wanted == code] for code in FIRST_LIMITS}
    ad_pass, ad_fall = select(pools["EXT_SPECIAL"], FIRST_LIMITS["EXT_SPECIAL"], key)
    ve_pass, ve_fall = select(pools["EXT_VETERANS"], FIRST_LIMITS["EXT_VETERANS"], key)
    spe_pass, spe_fall = select(pools["SPE"] + ad_fall + ve_fall, FIRST_LIMITS["SPE"], key)
    gen_pass, gen_fall = select(pools["GEN"] + spe_fall, FIRST_LIMITS["GEN"], key)

    applied = {}
    for group, code in [(ad_pass, "EXT_SPECIAL"), (ve_pass, "EXT_VETERANS"), (spe_pass, "SPE"), (gen_pass, "GEN")]:
        for a in group:
            applied[a.id] = code
    for a in gen_fall:
        applied[a.id] = None  # 1차 탈락
    return applied


def run_second(applicants, first_applied):
    """2차 평가: 미응시 제외 → 특례/보훈(초과분 SPE 편입) → 특별(초과분 GEN 편입, 미충원 이월) → 일반"""
    participants = [a for a in applicants if first_applied.get(a.id)]
    absent = [a.id for a in participants if a.comp is None or a.inter is None]
    active = [a for a in participants if a.comp is not None and a.inter is not None]

    key = lambda a: (-a.composite(), -a.comp, -a.inter) + a.common_tiebreak()

    pools = {code: [a for a in active if first_applied[a.id] == code] for code in SECOND_LIMITS}
    ad_pass, ad_fall = select(pools["EXT_SPECIAL"], SECOND_LIMITS["EXT_SPECIAL"], key)
    ve_pass, ve_fall = select(pools["EXT_VETERANS"], SECOND_LIMITS["EXT_VETERANS"], key)

    spe_pool = pools["SPE"] + ad_fall + ve_fall
    spe_pass, spe_fall = select(spe_pool, SECOND_LIMITS["SPE"], key)
    spe_unfilled = max(0, SECOND_LIMITS["SPE"] - len(spe_pool))

    gen_pass, gen_fall = select(pools["GEN"] + spe_fall, SECOND_LIMITS["GEN"] + spe_unfilled, key)
    if spe_unfilled:
        print("  (특별전형 미충원 %d명 → 일반전형 이월)" % spe_unfilled)

    applied = {}
    for group, code in [(ad_pass, "EXT_SPECIAL"), (ve_pass, "EXT_VETERANS"), (spe_pass, "SPE"), (gen_pass, "GEN")]:
        for a in group:
            applied[a.id] = code
    for a in gen_fall:
        applied[a.id] = None
    return applied, absent


def run_assignment(applicants, second_applied):
    """학과 배정: 최종 성적순으로 1~3지망, 정원내/정원외 풀 분리"""
    passers = [a for a in applicants if second_applied.get(a.id)]
    key = lambda a: (-a.composite(), -a.comp, -a.inter) + a.common_tiebreak()
    ordered = sorted(passers, key=key)
    for x, y in zip(ordered, ordered[1:]):
        assert key(x) != key(y), "배정 순서 완전 동점: %s vs %s" % (x.id, y.id)

    normal = dict(MAJOR_CAPACITY)
    extra = {m: EXTRA_CAP_PER_MAJOR for m in MAJOR_CAPACITY}
    majors = {}
    for a in ordered:
        pool = extra if second_applied[a.id] in ("EXT_SPECIAL", "EXT_VETERANS") else normal
        assigned = None
        for choice in a.choices:
            if pool[choice] > 0:
                pool[choice] -= 1
                assigned = choice
                break
        assert assigned, "배정 불가: %s" % a.id
        majors[a.id] = assigned
    return majors


# ── 지원자 생성 ─────────────────────────────────────────────────────────

def build_scenario(seed, name, spe_count, ged_count, absent_count, tie_pairs):
    rng = random.Random(seed)
    applicants = []
    counter = [0]

    def score3(lo_k, hi_k):
        """0.003 단위의 scale-3 점수"""
        return F(3 * rng.randint(lo_k, hi_k), 1000)

    def add(wanted, ged=False, absent=False, doc_override=None):
        counter[0] += 1
        att = F(3 * rng.randint(0, 10))  # 0~30, 3의 배수
        vol = F(3 * rng.randint(2, 10))  # 6~30, 3의 배수
        if ged:
            general = arts = subjects = None
            sems = {"3-1": None, "2-2": None, "2-1": None, "1-2": None}
            subjects_ged = score3(40000, 80000)  # 120~240
        else:
            general = score3(30000, 60000)  # 90~180
            arts = score3(10000, 20000)     # 30~60
            subjects_ged = None
            sems = {
                "3-1": F(rng.randint(40000, 72000), 1000),
                "2-2": F(rng.randint(25000, 45000), 1000),
                "2-1": F(rng.randint(25000, 45000), 1000),
                "1-2": F(rng.randint(10000, 18000), 1000),
            }
        if absent:
            score = F(rng.randint(400, 1000), 10)
            comp, inter = (None, score) if rng.random() < 0.5 else (score, None)
        else:
            comp = F(rng.randint(400, 1000), 10)
            inter = F(rng.randint(400, 1000), 10)
        choices = rng.sample(["SW", "IOT", "AI"], 3)

        a = Applicant("%s-%03d" % (name, counter[0]), wanted, ged, subjects_ged,
                      general, sems, arts, att, vol, comp, inter, choices)

        if doc_override is not None:
            assert not ged
            a.general += doc_override - a.doc  # 총점 동점 유도 (0.003 배수 차이라 정합 유지)

        # 합성점수(÷3×0.5)가 정확히 scale 3이 되도록 총점을 0.006의 배수로 보정
        if (a.doc / F(3, 1000)) % 2 == 1:
            if ged:
                a.subjects_ged += F(3, 1000)
            else:
                a.general += F(3, 1000)
        applicants.append(a)
        return a

    for _ in range(95):
        add("GEN")
    for _ in range(ged_count):
        add("GEN", ged=True)
    for _ in range(spe_count):
        add("SPE")
    add("SPE", ged=True)
    for _ in range(3):
        add("EXT_VETERANS")
    for _ in range(2):
        add("EXT_SPECIAL")
    for _ in range(absent_count):
        add("GEN", absent=True)
    for _ in range(tie_pairs):  # 1차 총점 동점 쌍 (동점자 기준으로만 갈리는 케이스)
        base = add("GEN")
        add("GEN", doc_override=base.doc)

    first = run_first(applicants)
    second, absent = run_second(applicants, first)
    majors = run_assignment(applicants, second)
    return name, applicants, first, second, absent, majors


# ── 리소스 파일 생성 ────────────────────────────────────────────────────
# 형식(파이프 구분, "-" = 값 없음):
#   S|시나리오명
#   A|id|지망전형|GED여부|총점|비교과|일반교과|3-1|2-2|2-1|1-2|역량|면접|지망학과(;구분)
#   F|id|1차 적용 전형        T|id|2차 적용 전형        C|id|2차 합성점수
#   B|id (2차 미응시)         M|id|배정 학과

def fmt3(x):
    n = x * 1000
    assert n.denominator == 1, "scale-3이 아닌 값: %s" % x
    n = int(n)
    return "%d.%03d" % (n // 1000, n % 1000)


def fmt1(x):
    n = int(x * 10)
    return "%d.%d" % (n // 10, n % 10)


def opt(v, fmt):
    return fmt(v) if v is not None else "-"


def emit_scenario(out, name, applicants, first, second, absent, majors):
    out.append("S|%s" % name)
    for a in applicants:
        out.append("A|%s|%s|%d|%s|%s|%s|%s|%s|%s|%s|%s|%s|%s" % (
            a.id, a.wanted, 1 if a.ged else 0, fmt3(a.doc), fmt3(a.non_subjects),
            opt(a.general, fmt3),
            opt(a.semesters["3-1"], fmt3), opt(a.semesters["2-2"], fmt3),
            opt(a.semesters["2-1"], fmt3), opt(a.semesters["1-2"], fmt3),
            opt(a.comp, fmt1), opt(a.inter, fmt1),
            ";".join(a.choices),
        ))
    by_id = {a.id: a for a in applicants}
    for id, v in first.items():
        out.append("F|%s|%s" % (id, v if v else "-"))
    for id, v in second.items():
        out.append("T|%s|%s" % (id, v if v else "-"))
        out.append("C|%s|%s" % (id, fmt3(by_id[id].composite())))
    for id in absent:
        out.append("B|%s" % id)
    for id, m in majors.items():
        out.append("M|%s|%s" % (id, m))


def main():
    scenarios = [
        build_scenario(seed=20260721, name="S1", spe_count=13, ged_count=4, absent_count=6, tie_pairs=2),
        build_scenario(seed=20260722, name="S2", spe_count=3, ged_count=3, absent_count=4, tie_pairs=1),  # 특별 미충원 이월
        build_scenario(seed=20260723, name="S3", spe_count=10, ged_count=2, absent_count=8, tie_pairs=2),
    ]

    out = ["# generated by tools/golden/generate_batch_golden.py"]
    for s in scenarios:
        emit_scenario(out, *s)
    with open(OUT, "w") as f:
        f.write("\n".join(out) + "\n")

    total = sum(len(s[1]) for s in scenarios)
    print("생성 완료: %s (시나리오 %d개, 지원자 %d명)" % (OUT, len(scenarios), total))
    for name, applicants, first, second, absent, majors in scenarios:
        first_pass = sum(1 for v in first.values() if v)
        second_pass = sum(1 for v in second.values() if v)
        print("  %s: 지원 %d → 1차 합격 %d → 미응시 %d → 최종 합격 %d (배정 %d)" %
              (name, len(applicants), first_pass, len(absent), second_pass, len(majors)))


if __name__ == "__main__":
    main()
