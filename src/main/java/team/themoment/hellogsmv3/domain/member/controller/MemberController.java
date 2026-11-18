package team.themoment.hellogsmv3.domain.member.controller;

import static team.themoment.hellogsmv3.domain.member.entity.type.AuthCodeType.*;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.member.dto.request.AuthenticateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.dto.request.CreateMemberReqDto;
import team.themoment.hellogsmv3.domain.member.dto.request.GenerateCodeReqDto;
import team.themoment.hellogsmv3.domain.member.dto.response.*;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.hellogsmv3.domain.member.service.*;
import team.themoment.hellogsmv3.domain.member.service.impl.GenerateCodeServiceImpl;
import team.themoment.hellogsmv3.domain.member.service.impl.GenerateTestCodeServiceImpl;
import team.themoment.hellogsmv3.global.common.handler.annotation.AuthRequest;
import team.themoment.hellogsmv3.global.security.auth.AuthenticatedUserManager;
import team.themoment.sdk.response.CommonApiResponse;

@Tag(name = "Member API", description = "멤버 관련 API입니다.")
@RestController
@RequestMapping("/member/v3")
@RequiredArgsConstructor
public class MemberController {

    private final AuthenticatedUserManager manager;
    private final GenerateCodeServiceImpl generateCodeService;
    private final GenerateTestCodeServiceImpl generateTestCodeService;
    private final AuthenticateCodeService authenticateCodeService;
    private final QueryMemberByIdService queryMemberByIdService;
    private final CreateMemberService createMemberService;
    private final QueryMemberAuthInfoByIdService queryMemberAuthInfoByIdService;
    private final QueryMyFirstTestResultService queryMyFirstTestResultService;
    private final QueryMySecondTestResultService queryMySecondTestResultService;
    private final QueryCheckDuplicateMemberService queryCheckDuplicateMemberService;

    @Operation(summary = "인증코드 전송", description = "6자리 가입 인증코드를 Redis에 저장하고 SMS로 전송합니다. 유효시간은 3분이며 동일 회원의 저장된 코드 기준 전송 횟수는 5회로 제한됩니다.\n권한: UNAUTHENTICATED, APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "전화번호 검증 실패 또는 인증코드 전송 5회 제한 초과", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @PostMapping("/member/me/send-code")
    public CommonApiResponse sendCode(@AuthRequest Long memberId, @RequestBody @Valid GenerateCodeReqDto reqDto) {
        generateCodeService.execute(memberId, reqDto);
        return CommonApiResponse.success("전송되었습니다.");
    }

    @Operation(summary = "인증코드 테스트 전송", description = "SMS를 보내지 않고 테스트 인증코드를 발급합니다. 횟수 제한이 없으며 저장된 전송 횟수를 0으로 초기화합니다. 코드는 data가 아닌 message(전송되었습니다. : 123456)에 포함됩니다.\n권한: ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "전화번호 검증 실패", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @PostMapping("/member/me/send-code-test")
    public CommonApiResponse sendCodeTest(@AuthRequest Long memberId, @RequestBody @Valid GenerateCodeReqDto reqDto) {
        String code = generateTestCodeService.execute(memberId, reqDto);
        return CommonApiResponse.success("전송되었습니다. : " + code);
    }

    @Operation(summary = "인증코드 인증", description = "가입 인증코드를 확인하고 인증 완료 상태로 저장합니다.\n권한: UNAUTHENTICATED, APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "요청 검증 실패 또는 인증코드 불일치", content = @Content()),
            @ApiResponse(responseCode = "404", description = "인증코드가 없거나 만료됨", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @PostMapping("/member/me/auth-code")
    public CommonApiResponse authCode(@AuthRequest Long memberId, @RequestBody @Valid AuthenticateCodeReqDto reqDto) {
        authenticateCodeService.execute(memberId, reqDto, SIGNUP);
        return CommonApiResponse.success("인증되었습니다.");
    }

    @Operation(summary = "내 멤버 조회", description = "내 멤버 정보를 조회합니다. 가입 전에는 이름, 전화번호, 생년월일, 성별이 null입니다.\n권한: UNAUTHENTICATED, APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @GetMapping("/member/me")
    public FoundMemberResDto find(@AuthRequest Long memberId) {
        return queryMemberByIdService.execute(memberId);
    }

    @Operation(summary = "멤버 조회", description = "멤버 ID로 멤버 정보를 조회합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "회원 ID 타입 오류", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @GetMapping("/member/{memberId}")
    public FoundMemberResDto findByMemberId(@PathVariable Long memberId) {
        return queryMemberByIdService.execute(memberId);
    }

    @Operation(summary = "내 멤버 등록", description = "인증 완료된 가입 코드와 멤버 정보로 등록합니다. 동일 전화번호의 기존 회원 및 원서가 있으면 삭제합니다. 중복 여부와 관계없이 원서접수 마감 이후 또는 전체 결과 중 1차 합격 여부가 하나라도 입력된 경우 등록할 수 없습니다. 성공하면 DB 역할을 APPLICANT로 저장하고 세션에 APPLICANT 권한을 추가합니다.\n권한: UNAUTHENTICATED, APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "201", description = "멤버 등록 성공"),
            @ApiResponse(responseCode = "400", description = "요청 검증 실패, 코드 없음/미인증/불일치, 인증 전화번호 불일치, 이미 가입한 지원자 또는 가입 제한 시점", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @PostMapping("/member/me")
    public CommonApiResponse create(HttpServletRequest httpServletRequest,
            @RequestBody @Valid CreateMemberReqDto reqDto,
            @AuthRequest Long memberId) {
        Role role = createMemberService.execute(reqDto, memberId);
        manager.setRole(httpServletRequest, role);
        return CommonApiResponse.created("본인인증이 완료되었습니다.");
    }

    @Operation(summary = "내 인증 정보 조회", description = "내 OAuth 인증 정보와 역할을 조회합니다.\n권한: UNAUTHENTICATED, APPLICANT, ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @GetMapping("/auth-info/me")
    public FoundMemberAuthInfoResDto findAuthInfo(@AuthRequest Long memberId) {
        return queryMemberAuthInfoByIdService.execute(memberId);
    }

    @Operation(summary = "인증 정보 조회", description = "멤버 ID로 OAuth 인증 정보와 역할을 조회합니다.\n권한: ADMIN, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "회원 ID 타입 오류", content = @Content()),
            @ApiResponse(responseCode = "404", description = "회원 없음", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @GetMapping("/auth-info/{memberId}")
    public FoundMemberAuthInfoResDto findAuthInfoByMemberId(@PathVariable Long memberId) {
        return queryMemberAuthInfoByIdService.execute(memberId);
    }

    @Operation(summary = "1차 전형 결과 조회", description = "본인의 1차 전형 결과를 조회합니다. 결과 설정이 없거나 1차 발표 플래그가 NO이거나, 1차 발표 시각 전이거나 전체 원서 중 1차 합격 여부가 null인 결과가 있으면 204(본문 없음)입니다.\n권한: APPLICANT, ROOT")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "1차 전형 결과 조회 성공"),
            @ApiResponse(responseCode = "204", description = "전체 발표 조건 미충족(본문 없음)", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content()),
            @ApiResponse(responseCode = "404", description = "원서 없음", content = @Content())})
    @GetMapping("/first-test-result/me")
    public FoundMemberFirstTestResDto firstTestResult(@AuthRequest Long memberId) {
        return queryMyFirstTestResultService.execute(memberId);
    }

    @Operation(summary = "2차 전형 결과 조회", description = "본인의 2차 전형 결과를 조회합니다. 결과 설정이 없거나 2차 발표 플래그가 NO이거나, 1차 발표 시각 전이거나 전체 원서 중 1차 합격 여부가 null인 결과가 있으면 204(본문 없음)입니다.\n권한: APPLICANT, ROOT")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "2차 전형 결과 조회 성공"),
            @ApiResponse(responseCode = "204", description = "전체 발표 조건 미충족(본문 없음)", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content()),
            @ApiResponse(responseCode = "404", description = "원서 없음", content = @Content())})
    @GetMapping("/second-test-result/me")
    public FoundMemberSecondTestResDto secondTestResult(@AuthRequest Long memberId) {
        return queryMySecondTestResultService.execute(memberId);
    }

    @Operation(summary = "중복 회원가입 여부 확인", description = "가입된 지원자의 전화번호인지 확인합니다. phoneNumber는 필수 쿼리이며 형식 검증은 수행하지 않습니다.\n권한: UNAUTHENTICATED, ROOT")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "요청 성공"),
            @ApiResponse(responseCode = "400", description = "전화번호 쿼리 누락", content = @Content()),
            @ApiResponse(responseCode = "401", description = "로그인 필요", content = @Content()),
            @ApiResponse(responseCode = "403", description = "권한 부족", content = @Content())})
    @GetMapping("/check-duplicate")
    public FoundDuplicateMemberResDto duplicateCheck(@RequestParam String phoneNumber) {
        return queryCheckDuplicateMemberService.execute(phoneNumber);
    }
}
