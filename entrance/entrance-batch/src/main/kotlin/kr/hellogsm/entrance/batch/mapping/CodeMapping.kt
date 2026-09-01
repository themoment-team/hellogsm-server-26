package kr.hellogsm.entrance.batch.mapping

import team.themoment.hellogsmv3.domain.oneseo.entity.type.Major
import team.themoment.hellogsmv3.domain.oneseo.entity.type.Screening

/**
 * 엔티티 enum ↔ 요강(plan) 코드 문자열 변환.
 *
 * 엔진은 전형·학과를 문자열 코드로 다루고(plan 의 선언 코드), DB 는 enum 으로 저장한다.
 * - 전형: GENERAL→GEN, SPECIAL→SPE, EXTRA_VETERANS→EXT_VETERANS, EXTRA_ADMISSION→EXT_SPECIAL
 * - 학과: enum 이름이 곧 plan 코드(SW/IOT/AI)
 */
object CodeMapping {

    fun screeningCode(screening: Screening): String = when (screening) {
        Screening.GENERAL -> "GEN"
        Screening.SPECIAL -> "SPE"
        Screening.EXTRA_VETERANS -> "EXT_VETERANS"
        Screening.EXTRA_ADMISSION -> "EXT_SPECIAL"
    }

    fun toScreening(code: String): Screening = when (code) {
        "GEN" -> Screening.GENERAL
        "SPE" -> Screening.SPECIAL
        "EXT_VETERANS" -> Screening.EXTRA_VETERANS
        "EXT_SPECIAL" -> Screening.EXTRA_ADMISSION
        else -> throw IllegalArgumentException("알 수 없는 전형 코드: $code")
    }

    fun majorCode(major: Major): String = major.name

    fun toMajor(code: String): Major = Major.valueOf(code)
}
