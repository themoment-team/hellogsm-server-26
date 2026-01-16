package team.themoment.hellogsmv3.global.security.auth;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import team.themoment.hellogsmv3.global.common.response.CommonApiResponse;
import team.themoment.hellogsmv3.global.exception.error.ExpectedException;
import team.themoment.hellogsmv3.global.security.auth.dto.request.OAuthCodeReqDto;
import team.themoment.hellogsmv3.global.security.auth.service.OAuthAuthenticationService;

@Tag(name = "Auth API", description = "인증 관련 API입니다.")
@RestController
@RequestMapping("/auth/v3")
@RequiredArgsConstructor
public class AuthController {

    private final OAuthAuthenticationService oAuthAuthenticationService;
    private final ObjectMapper objectMapper;

    @Operation(summary = "OAuth 인증", description = "프론트엔드에서 받은 Authorization Code로 인증을 처리합니다.")
    @PostMapping("/auth/{provider}")
    public CommonApiResponse authenticateWithOAuth(@PathVariable String provider,
            @RequestBody @Valid OAuthCodeReqDto reqDto,
            HttpServletRequest request) {
        oAuthAuthenticationService.execute(provider, reqDto.code(), request);
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
    @Operation(summary = "로그아웃", description = "로그아웃을 진행합니다.")
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
