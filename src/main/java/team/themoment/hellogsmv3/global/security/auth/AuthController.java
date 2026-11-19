package team.themoment.hellogsmv3.global.security.auth;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.global.config.SessionSecurityOpenApiConfig;
import team.themoment.hellogsmv3.global.security.auth.dto.request.OAuthCodeReqDto;
import team.themoment.hellogsmv3.global.security.auth.service.OAuthAuthenticationService;
import team.themoment.sdk.exception.ExpectedException;
import team.themoment.sdk.response.CommonApiResponse;
import tools.jackson.databind.ObjectMapper;

@Tag(name = "Auth API", description = "인증 관련 API입니다.")
@RestController
@RequestMapping("/auth/v3")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthAuthenticationService oAuthAuthenticationService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "OAuth 인증", description = "Authorization Code를 토큰으로 교환하고 OAuth 회원을 조회합니다. 최초 로그인은 UNAUTHENTICATED 회원을 자동 생성합니다. SecurityContext를 서버 세션에 저장하며 302 리다이렉트 없이 200 JSON(data 없음)과 SESSION 쿠키를 반환합니다. 기존 유효 세션은 재사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공(JSON 및 세션 쿠키)", headers = @Header(name = "Set-Cookie", description = "서버 세션 쿠키 SESSION 발급(기존 세션 재사용 시 생략 가능)", schema = @Schema(type = "string", example = "SESSION=세션값; Path=/; HttpOnly"))),
            @ApiResponse(responseCode = "400", description = "필수 필드 검증 실패 또는 지원하지 않는 provider", content = @Content()),
            @ApiResponse(responseCode = "401", description = "토큰 교환, 사용자 정보 조회 또는 사용자 식별 정보 확인 실패", content = @Content()),
            @ApiResponse(responseCode = "500", description = "OAuth 클라이언트 등록 누락 등 서버 오류", content = @Content())})
    @PostMapping("/auth/{provider}")
    public CommonApiResponse authenticateWithOAuth(
            @Parameter(description = "OAuth 제공자(대소문자 무시)", schema = @Schema(allowableValues = {"google",
                    "kakao"})) @PathVariable String provider,
            @RequestBody @Valid OAuthCodeReqDto reqDto,
            HttpServletRequest request) {
        oAuthAuthenticationService.execute(provider, reqDto.code(), reqDto.redirectUri(), request);
        return CommonApiResponse.success("인증이 완료되었습니다.");
    }

    /**
     * 로그아웃 시도 시 세션 저장을 시도하는 Spring Session의 기본 동작으로 인해 이미 응답이 커밋된 후에 예외가 발생하는 문제가
     * 있었습니다. 이를 해결하기 위해 응답을 직접 작성하는 방식을 사용하여 세션 저장 시도를 우회하였습니다. 해당 메서드에서는 직접 응답을
     * 작성한 후 커밋하여 무효화된 세션이 저장되지 않도록 합니다.
     *
     * @param req
     *            HTTP 요청 객체
     * @param res
     *            HTTP 응답 객체
     * @throws IOException
     *             JSON 응답 작성 중 발생할 수 있는 예외
     * @see <a href=
     *      "https://github.com/themoment-team/hellogsm-server-25/pull/325">관련
     *      PR</a>
     * @author snowykte0426
     */
    @Operation(summary = "로그아웃", description = "OAuth2 인증 세션이 필요합니다. 컨트롤러가 200 JSON을 직접 작성한 뒤 서버 세션을 무효화하고 SecurityContext를 정리합니다. 클라이언트 쿠키 자체를 명시적으로 삭제하지는 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공(직접 작성한 JSON, data 없음)", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonApiResponse.class), examples = @ExampleObject(value = "{\"status\":\"200 OK\",\"code\":200,\"message\":\"로그아웃 되었습니다.\"}"))),
            @ApiResponse(responseCode = "401", description = "세션이 없거나 OAuth2 인증이 아님", content = @Content())})
    @SecurityRequirement(name = SessionSecurityOpenApiConfig.SESSION_COOKIE_SCHEME)
    @GetMapping("/logout")
    public void logout(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof OAuth2AuthenticationToken)) {
            throw new ExpectedException("인증 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED);
        }
        CommonApiResponse response = CommonApiResponse.success("로그아웃 되었습니다.");
        writeJsonResponse(res, HttpServletResponse.SC_OK, response); // Spring MVC의 일반적인 응답 처리 방식을 우회하여 세션 저장 시도 문제 수정
        new SecurityContextLogoutHandler().logout(req, res, auth);
    }

    /**
     * JSON 응답을 HttpServletResponse에 직접 작성하는 유틸리티 메서드입니다. Spring MVC의 일반적인 응답 처리 방식을
     * 우회하여 즉시 응답을 클라이언트에게 전송합니다.
     *
     * @param res
     *            HTTP 응답 객체
     * @param status
     *            HTTP 상태 코드
     * @param responseObj
     *            JSON으로 직렬화할 응답 객체
     * @throws IOException
     *             JSON 작성 또는 응답 플러시 중 발생할 수 있는 예외
     * @author snowykte0426
     */
    private void writeJsonResponse(HttpServletResponse res, int status, Object responseObj) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(objectMapper.writeValueAsString(responseObj));
        res.flushBuffer();
    }
}
