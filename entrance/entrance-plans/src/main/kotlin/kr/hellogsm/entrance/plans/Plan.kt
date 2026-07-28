package kr.hellogsm.entrance.plans

import kr.hellogsm.entrance.dsl.admissionPlan
import kr.hellogsm.entrance.plan.Achievement.A
import kr.hellogsm.entrance.plan.Achievement.B
import kr.hellogsm.entrance.plan.Achievement.C
import kr.hellogsm.entrance.plan.Achievement.D
import kr.hellogsm.entrance.plan.Achievement.E
import kr.hellogsm.entrance.plan.GraduationType.CANDIDATE
import kr.hellogsm.entrance.plan.GraduationType.GED
import kr.hellogsm.entrance.plan.GraduationType.GRADUATE
import kr.hellogsm.entrance.plan.MissingSemesterStrategy.LOWER_YEAR
import kr.hellogsm.entrance.plan.MissingSemesterStrategy.SAME_YEAR_OTHER_SEMESTER
import kr.hellogsm.entrance.plan.MissingSemesterStrategy.UPPER_YEAR
import java.time.LocalDate

/**
 * 현재 활성 요강 — 2026학년도 광주소프트웨어마이스터고등학교 입학전형요강.
 *
 * 이 파일(`Plan.kt`)은 항상 "지금 쓰는 plan"을 가리키는 고정 이름이다. 소비자(`entrance-batch`,
 * `entrance-lambda` 등)는 연도를 몰라도 `kr.hellogsm.entrance.plans.plan`만 참조하면 된다.
 * 새 학년도로 넘어갈 때는 이 파일을 고치는 게 아니라 ① 지금 내용을 `legacy/PlanXXXX.kt`로
 * 옮기고 심볼명을 `planXXXX`로 바꿔 얼린 뒤 ② 이 파일을 새 연도 내용으로 덮어쓴다
 * (`legacy/README.md`, `entrance/CLAUDE.md` 참고).
 *
 * 근거 문서: `.reference/2026_entrance.pdf` (2025. 7. 11. 공고)
 */
val plan = admissionPlan(year = 2026) {
    majors {
        major("SW", "소프트웨어개발과", capacity = 36)
        major("IOT", "스마트IoT과", capacity = 18)
        major("AI", "인공지능(AI)과", capacity = 18)
    }

    screenings {
        // 일반전형 64명 = 총정원 72 − 특별전형 8 (특별전형 미충원 시 여기서 추가 선발)
        regular("GEN", "일반전형")

        regular("SPE", "특별전형(사회통합전형)") {
            quota(8)
            unfilledGoesTo("GEN")
            rejectedFallsTo("GEN")
            subType("BASIC_LIVING", "기초생활수급자")
            subType("NEAR_POVERTY", "차상위계층")
            subType("MULTICULTURAL", "다문화가정")
            subType("SINGLE_PARENT", "한부모가족")
            subType("WELFARE_FACILITY", "아동복지시설 보호아동")
        }

        extra("EXT_VETERANS", "국가보훈대상자") {
            quota(2, capPercentOfTotal = 3)
            admitOnlyWithinFirstRoundCutline()
            overflowFallsTo("SPE")
        }

        extra("EXT_SPECIAL", "특례입학대상자") {
            quota(1, capPercentOfTotal = 2)
            admitOnlyWithinFirstRoundCutline()
            overflowFallsTo("SPE")
        }
    }

    grading {
        // 중간값: 소수점 여섯째 자리에서 반올림해 다섯째 자리까지 / 결과값: 넷째 자리에서 반올림
        rounding(intermediateScale = 5, resultScale = 3)

        transcript(CANDIDATE) {
            generalSubjects(max = 180) {
                achievement(A to 5, B to 4, C to 3, D to 2, E to 1)
                semester(1, 2, points = 18)
                semester(2, 1, points = 45)
                semester(2, 2, points = 45)
                semester(3, 1, points = 72)
                missingSemester(SAME_YEAR_OTHER_SEMESTER, UPPER_YEAR, LOWER_YEAR)
            }
            artsSubjects(max = 60) {
                achievement(A to 5, B to 4, C to 3)
            }
            attendance(max = 30) {
                latenessPerAbsenceDay = 3
                deductionPerAbsenceDay = 3
                zeroFromAbsenceDays = 10
                missingYearDefault = 5
            }
            volunteer(maxPerYear = 10) {
                step(minHours = 7, points = 10)
                step(minHours = 6, points = 8)
                step(minHours = 5, points = 6)
                step(minHours = 4, points = 4)
                floor(2)
                missingYearDefault = 2
            }
        }

        transcript(GRADUATE) {
            generalSubjects(max = 180) {
                achievement(A to 5, B to 4, C to 3, D to 2, E to 1)
                semester(2, 1, points = 36)
                semester(2, 2, points = 36)
                semester(3, 1, points = 54)
                semester(3, 2, points = 54)
                missingSemester(SAME_YEAR_OTHER_SEMESTER, UPPER_YEAR, LOWER_YEAR)
            }
            artsSubjects(max = 60) {
                achievement(A to 5, B to 4, C to 3)
            }
            attendance(max = 30) {
                latenessPerAbsenceDay = 3
                deductionPerAbsenceDay = 3
                zeroFromAbsenceDays = 10
                missingYearDefault = 5
            }
            volunteer(maxPerYear = 10) {
                step(minHours = 7, points = 10)
                step(minHours = 6, points = 8)
                step(minHours = 5, points = 6)
                step(minHours = 4, points = 4)
                floor(2)
                missingYearDefault = 2
            }
        }

        // 검정고시 환산식 근거: 요강 p.26 (부록 3, 5-나항) 수식 원문 및
        // go-hellogsm-score-calculator 2026 시즌 코드(커밋 4e8d668 이전)와 대조하여 확정.
        formula(GED) {
            // 교과 = (검정고시 평균점수 − 50) ÷ 50 × 240
            subjects(minInput = 50, maxInput = 100, maxScore = 240)
            attendanceFixed(30)
            // 봉사 = (검정고시 평균점수 − 40) ÷ 60 × 30
            volunteer(minInput = 40, maxInput = 100, maxScore = 30)
        }
    }

    rounds {
        round("FIRST", "1차 전형(서류전형)") {
            selectByMultiplier(1.3)
            sumScore(subjectScore, attendanceScore, volunteerScore) // 300점
            tiebreak {
                byGeneralSubjectScore()
                bySemesters(3 to 1, 2 to 2, 2 to 1, 1 to 2)
                byNonSubjectScore()
            }
        }

        round("SECOND", "2차 전형(역량검사·심층면접)") {
            selectByCapacity()
            val competency = manualScore("COMPETENCY", "역량검사", max = 100)
            val interview = manualScore("INTERVIEW", "심층면접", max = 100)
            weightedScore(max = 100) {
                part(roundScore("FIRST"), weightPercent = 50, normalizeTo = 100)
                part(competency, weightPercent = 30)
                part(interview, weightPercent = 20)
            }
            tiebreak {
                byManualScore("COMPETENCY")
                byManualScore("INTERVIEW")
                byGeneralSubjectScore()
                bySemesters(3 to 1, 2 to 2, 2 to 1, 1 to 2)
                byNonSubjectScore()
            }
        }
    }

    majorAssignment {
        choiceCount = 3
        extraScreeningCapPerMajor = 2
    }

    waitlist {
        percentOfTotal(3)
        from("GEN")
    }

    additionalRecruitment {
        screening("GEN")
        basedOnRound("FIRST")
    }

    schedule {
        event("APPLICATION", "원서 접수 및 증빙서류 제출", LocalDate.of(2025, 10, 20), LocalDate.of(2025, 10, 23))
        event("FIRST_RESULT", "1차 전형 합격자 발표", LocalDate.of(2025, 10, 28))
        event("COMPETENCY_TEST", "2차 전형 역량검사", LocalDate.of(2025, 10, 31))
        event("INTERVIEW", "2차 전형 심층면접", LocalDate.of(2025, 11, 1))
        event("FINAL_RESULT", "2차(최종) 합격자 발표", LocalDate.of(2025, 11, 5))
        event("ENROLLMENT", "합격자 등록", LocalDate.of(2025, 11, 5), LocalDate.of(2025, 11, 10))
        event("ADDITIONAL_APPLICATION", "추가모집 원서접수", LocalDate.of(2025, 11, 11), LocalDate.of(2025, 11, 12))
        event("ADDITIONAL_RESULT", "추가모집 합격자 발표 및 등록", LocalDate.of(2025, 11, 14), LocalDate.of(2025, 11, 19))
    }
}
