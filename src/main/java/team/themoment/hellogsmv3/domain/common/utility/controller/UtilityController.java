package team.themoment.hellogsmv3.domain.common.utility.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.domain.common.utility.service.DeleteMemberService;
import team.themoment.hellogsmv3.domain.common.utility.service.DeleteOneseoService;
import team.themoment.hellogsmv3.domain.common.utility.service.ModifyMemberRoleService;
import team.themoment.hellogsmv3.domain.member.entity.type.Role;
import team.themoment.sdk.response.CommonApiResponse;

@RestController
@RequestMapping("/utility/v3")
@Tag(name = "Utility API", description = "개발용 유틸리티 API입니다. prod 프로파일에서는 컨트롤러가 등록되지 않습니다. 인증 없이 호출할 수 있습니다.")
@RequiredArgsConstructor
@Profile("!prod")
public class UtilityController {

    private final DeleteOneseoService deleteOneseoService;
    private final DeleteMemberService deleteMemberService;
    private final ModifyMemberRoleService modifyMemberRoleService;

    @Operation(summary = "원서 삭제", description = "입력된 접수 번호에 해당하는 원서를 삭제합니다. prod 프로파일에서는 컨트롤러가 등록되지 않습니다. 인증 없이 호출할 수 있습니다.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "원서 삭제 성공. message에 접수 번호를 포함합니다."),
            @ApiResponse(responseCode = "400", description = "필수 쿼리 파라미터가 누락되었습니다.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "해당 접수 번호에 해당하는 원서가 존재하지 않음", content = @Content())})
    @DeleteMapping("/oneseo")
    public CommonApiResponse deleteOneseo(@RequestParam String submitCode) {
        deleteOneseoService.execute(submitCode);
        return CommonApiResponse.success("입력된 접수 번호에 해당하는 원서를 삭제했습니다. 접수 번호: " + submitCode);
    }

    @Operation(summary = "회원 탈퇴", description = "입력된 전화 번호에 해당하는 계정과 원서 및 모든 유형의 인증코드를 삭제합니다. 기존 세션은 무효화하지 않습니다. prod 프로파일에서는 컨트롤러가 등록되지 않습니다. 인증 없이 호출할 수 있습니다.")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "회원·원서·인증코드 삭제 성공. message에 전화 번호를 포함합니다."),
            @ApiResponse(responseCode = "400", description = "필수 쿼리 파라미터가 누락되었습니다.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "해당 전화 번호에 해당하는 계정이 존재하지 않음", content = @Content())})
    @DeleteMapping("/member")
    public CommonApiResponse deleteMember(@RequestParam String phoneNumber) {
        deleteMemberService.execute(phoneNumber);
        return CommonApiResponse.success("입력된 전화 번호에 해당하는 계정을 탈퇴했습니다. 전화 번호: " + phoneNumber);
    }

    @Operation(summary = "사용자 권한 수정", description = "입력된 전화 번호에 해당하는 사용자의 DB 권한을 수정합니다. 기존 세션 권한은 변경되지 않으며 재로그인해야 반영됩니다. prod 프로파일에서는 컨트롤러가 등록되지 않습니다. 인증 없이 호출할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "사용자 권한 수정 성공. message에 전화 번호와 변경한 role을 포함합니다."),
            @ApiResponse(responseCode = "400", description = "필수 쿼리 파라미터 누락 또는 role 값이 유효하지 않습니다.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "해당 전화 번호에 해당하는 계정이 존재하지 않음", content = @Content())})
    @PatchMapping("/member/role")
    public CommonApiResponse updateMemberRole(@RequestParam String phoneNumber, @RequestParam Role role) {
        modifyMemberRoleService.execute(phoneNumber, role);
        return CommonApiResponse.success("입력된 전화 번호에 해당하는 사용자의 권한을 수정했습니다. 전화 번호: " + phoneNumber + ", 권한: " + role);
    }
}
