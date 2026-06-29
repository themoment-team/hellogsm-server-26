package team.themoment.hellogsmv3.global.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer;

class SessionConfigTest {

    @Test
    void it_sets_shared_domain_for_configured_domain_subdomains() {
        CookieSerializer serializer = createCookieSerializer("stage.hellogsm.kr");

        String setCookie = writeCookie(serializer, "www.stage.hellogsm.kr");

        assertTrue(setCookie.contains("Domain=stage.hellogsm.kr"));
    }

    @Test
    void it_omits_domain_for_localhost() {
        CookieSerializer serializer = createCookieSerializer("stage.hellogsm.kr");

        String setCookie = writeCookie(serializer, "localhost");

        assertFalse(setCookie.contains("Domain="));
    }

    private CookieSerializer createCookieSerializer(String cookieDomain) {
        ServerProperties serverProperties = new ServerProperties();
        Cookie cookie = serverProperties.getServlet().getSession().getCookie();
        cookie.setName("SESSION");
        cookie.setDomain(cookieDomain);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSameSite(Cookie.SameSite.NONE);
        cookie.setSecure(true);

        return new SessionConfig(serverProperties, new MockEnvironment()).cookieSerializer();
    }

    private String writeCookie(CookieSerializer serializer, String serverName) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName(serverName);
        request.setSecure(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        serializer.writeCookieValue(new CookieSerializer.CookieValue(request, response, "session-id"));

        return response.getHeader("Set-Cookie");
    }
}
