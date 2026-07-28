# legacy — 지난 연도 plan 보관소

`kr.hellogsm.entrance.plans.plan`(`Plan.kt`)은 항상 **현재 활성 요강**만 가리키는 고정 이름이다.
새 학년도로 넘어갈 때 절차:

1. 지금의 `Plan.kt` 내용을 이 폴더에 `PlanXXXX.kt`로 옮기고, 심볼명을 `val plan` → `val planXXXX`로
   바꿔 얼린다(패키지는 `kr.hellogsm.entrance.plans.legacy`). 같은 이름의 테스트(`PlanTest.kt`)도
   `legacy/PlanXXXXTest.kt`로 함께 옮긴다.
2. `Plan.kt`를 새 학년도 요강 내용으로 덮어쓴다(`val plan = admissionPlan(year = XXXX) { ... }`).
3. `entrance-batch`/`entrance-lambda` 등 소비자 코드는 고치지 않는다 — `kr.hellogsm.entrance.plans.plan`
   임포트 경로가 그대로이므로 자동으로 새 plan을 쓰게 된다.

과거 plan을 보존하는 이유는 재현성(과거 시즌 배치를 다시 돌려 결과를 검증할 수 있어야 함) 때문이다.
`legacy/`에 옮긴 뒤에는 값을 수정하지 않는다 — 필요하면 새 파일을 추가한다.

상세 규칙은 [`entrance/CLAUDE.md`](../../../../../../../../../CLAUDE.md) 참고.
