package librarymanagement.config;

import jakarta.servlet.http.Cookie;
import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.context.WebApplicationContext;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

class CsrfTest extends IntegrationTest {

    private static final String TOKEN_COOKIE = "XSRF-TOKEN";
    private static final String TOKEN_HEADER = "X-XSRF-TOKEN";

    @Autowired
    private WebApplicationContext context;

    private MockMvcTester browser;

    @BeforeEach
    void setUpBrowser() {
        browser = MockMvcTester.from(context, builder -> builder.apply(springSecurity()).build());
    }

    @Test
    void testServerIssuesTokenThatPagesCanRead() {
        MvcTestResult result = browser.get().uri("/api/whoami").exchange();

        Cookie cookie = result.getResponse().getCookie(TOKEN_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isNotEmpty();
        assertThat(cookie.isHttpOnly()).isFalse();
    }

    @Test
    void testLoginWriteAndLogoutWithIssuedTokens() {
        librarianSession(); // creates the librarian
        String tokenBeforeLogin = issuedToken(browser.get().uri("/api/whoami").exchange());

        MvcTestResult login = browser.post()
                .uri("/api/login")
                .cookie(new Cookie(TOKEN_COOKIE, tokenBeforeLogin))
                .header(TOKEN_HEADER, tokenBeforeLogin)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=" + URLEncoder.encode(librarianEmail, StandardCharsets.UTF_8)
                        + "&password=" + URLEncoder.encode(librarianPassword, StandardCharsets.UTF_8))
                .exchange();
        assertThat(login).hasStatus(HttpStatus.OK);
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        String tokenAfterLogin = issuedToken(login);
        assertThat(tokenAfterLogin).isNotEqualTo(tokenBeforeLogin);

        assertThat(writeWithTokens(session, tokenAfterLogin, tokenAfterLogin)).hasStatus(HttpStatus.CREATED);
        assertThat(writeWithTokens(session, tokenAfterLogin, tokenBeforeLogin)).hasStatus(HttpStatus.FORBIDDEN);

        MvcTestResult logout = browser.post()
                .uri("/api/logout")
                .session(session)
                .cookie(new Cookie(TOKEN_COOKIE, tokenAfterLogin))
                .header(TOKEN_HEADER, tokenAfterLogin)
                .exchange();
        assertThat(logout).hasStatus(HttpStatus.NO_CONTENT);
        Cookie clearedCookie = logout.getResponse().getCookie(TOKEN_COOKIE);
        assertThat(clearedCookie).isNotNull();
        assertThat(clearedCookie.getMaxAge()).isZero();
    }

    @Test
    void testLoginWithoutTokenIsForbidden() {
        librarianSession(); // creates the librarian

        MvcTestResult result = browser.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=" + URLEncoder.encode(librarianEmail, StandardCharsets.UTF_8)
                        + "&password=" + URLEncoder.encode(librarianPassword, StandardCharsets.UTF_8))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result.getRequest().getSession(false)).isNull();
    }

    @Test
    void testWriteWithoutTokenIsForbidden() {
        MockHttpSession librarianSession = librarianSession();

        MvcTestResult result = browser.post()
                .uri("/api/admin/customers")
                .session(librarianSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName": "Joe", "lastName": "Mama"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }

    @Test
    void testWriteWithHeaderNotMatchingCookieIsForbidden() {
        MockHttpSession librarianSession = librarianSession();
        String token = issuedToken(browser.get().uri("/api/whoami").exchange());

        assertThat(writeWithTokens(librarianSession, token, "other-token")).hasStatus(HttpStatus.FORBIDDEN);
    }

    private MvcTestResult writeWithTokens(MockHttpSession session, String cookieToken, String headerToken) {
        return browser.post()
                .uri("/api/admin/customers")
                .session(session)
                .cookie(new Cookie(TOKEN_COOKIE, cookieToken))
                .header(TOKEN_HEADER, headerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName": "Joe", "lastName": "Mama"}
                        """)
                .exchange();
    }

    private static String issuedToken(MvcTestResult result) {
        for (Cookie cookie : result.getResponse().getCookies()) {
            if (cookie.getName().equals(TOKEN_COOKIE) && !cookie.getValue().isEmpty()) {
                return cookie.getValue();
            }
        }
        return fail("The response set no " + TOKEN_COOKIE + " cookie");
    }
}
