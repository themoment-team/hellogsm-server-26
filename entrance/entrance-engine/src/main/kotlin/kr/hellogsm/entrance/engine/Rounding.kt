package kr.hellogsm.entrance.engine

import kr.hellogsm.entrance.plan.RoundingPolicy
import java.math.BigDecimal

/**
 * 결과값 반올림 — plan의 [RoundingPolicy]가 정한 자리수·모드로 확정한다.
 *
 * scoring·evaluation 두 엔진이 같은 정책으로 반올림해야 하므로(어긋나면 차수 점수와 성적 총점의
 * 반올림 동작이 갈라진다) 정의를 이 한 곳에 둔다.
 */
internal fun RoundingPolicy.result(value: BigDecimal): BigDecimal = value.setScale(resultScale, resultMode)
