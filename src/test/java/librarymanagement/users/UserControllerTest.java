package librarymanagement.users;

import librarymanagement.constants.Messages;
import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

class UserControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testRegisterCustomer() {
        MockHttpSession librarianSession = librarianSession();
        long customerId = createCustomer(librarianSession, "Joe", "Mama");
        String code = activationCode(librarianSession, customerId);

        MvcTestResult result = register(code, "customer1@example.com", "customer123");

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");
        assertThat(result).bodyJson().extractingPath("customer.id").isEqualTo((int) customerId);
        assertThat(result).bodyJson().extractingPath("customer.firstName").isEqualTo("Joe");
        assertThat(result).bodyJson().doesNotHavePath("password");
    }

    @Test
    void testRegisterIgnoresRequestedRole() {
        MockHttpSession librarianSession = librarianSession();
        String code = activationCode(librarianSession, createCustomer(librarianSession, "Joe", "Evil"));

        MvcTestResult result = registerJson("""
                {
                    "activationCode": "%s",
                    "email": "evil@example.com",
                    "password": "password123",
                    "role": "ROLE_LIBRARIAN"
                }
                """.formatted(code));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");

        MvcTestResult staffLogin = mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=evil@example.com&password=password123")
                .exchange();
        assertThat(staffLogin).hasStatus(HttpStatus.FOUND);
        assertThat(mockMvcTester.get().uri("/admin").session(sessionOf(staffLogin))).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void testLoginLibrarian() {
        librarianSession();

        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=" + librarianEmail + "&password=" + librarianPassword)
                .exchange();

        assertThat(loginResult)
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/admin");
    }

    @Test
    void testLoginCustomer() {
        assertThat(registerNewCustomer("goober@example.com", "password123")).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=goober@example.com&password=password123")
                .exchange();

        assertThat(loginResult).hasStatus(HttpStatus.OK).hasContentType(MediaType.APPLICATION_JSON);
        assertThat(loginResult).bodyJson().extractingPath("email").isEqualTo("goober@example.com");
        assertThat(loginResult).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void testLibrarianLoginThroughApiLogin() {
        librarianSession();

        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=" + librarianEmail + "&password=" + librarianPassword)
                .exchange();

        assertThat(loginResult).hasStatus(HttpStatus.OK);
        assertThat(loginResult).bodyJson().extractingPath("role").isEqualTo("ROLE_LIBRARIAN");
        assertThat(mockMvcTester.get().uri("/api/whoami").session(sessionOf(loginResult)))
                .bodyJson()
                .extractingPath("role")
                .isEqualTo("ROLE_LIBRARIAN");
    }

    @Test
    void testRegisterCustomerMissingFields() {
        MvcTestResult result = registerJson("{}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertFieldError(result, "activationCode", Messages.ACTIVATION_CODE_VALIDATION_MESSAGE);
        assertFieldError(result, "email", Messages.USER_EMAIL_VALIDATION_MESSAGE);
        assertFieldError(result, "password", Messages.USER_PASSWORD_VALIDATION_MESSAGE);
    }

    @Test
    void testRegisterTooLongEmail() {
        MvcTestResult result = registerNewCustomer("a".repeat(146) + "@x.io", "secret123");

        assertFieldError(result, "email", Messages.USER_EMAIL_LENGTH_VALIDATION_MESSAGE);
    }

    @Test
    void testRegisterTooLongPassword() {
        // 40 characters, but 80 bytes in UTF-8
        MvcTestResult result = registerNewCustomer("customer1@example.com", "ą".repeat(40));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.USER_PASSWORD_TOO_LONG);
    }

    @Test
    void testRegisterDuplicateEmail() {
        assertThat(registerNewCustomer("customer1@example.com", "secret123")).hasStatus(HttpStatus.CREATED);

        MvcTestResult result = registerNewCustomer("CUSTOMER1@example.com", "secret123");

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.USER_EMAIL_DUPLICATE + "customer1@example.com");
    }

    @Test
    void testLoginIgnoresEmailCase() {
        assertThat(registerNewCustomer("Goober@Example.COM", "password123"))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("email")
                .isEqualTo("goober@example.com");

        assertThat(mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=GOOBER@example.com&password=password123"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("email")
                .isEqualTo("goober@example.com");
    }

    @Test
    void testCreateLibrarianInvalidEmail() {
        MvcTestResult result = createLibrarian(librarianSession(), "not-an-email", "secret123");

        assertFieldError(result, "email", Messages.USER_EMAIL_VALIDATION_MESSAGE);
    }

    @Test
    void testCreateLibrarian() {
        MockHttpSession librarianSession = librarianSession();

        MvcTestResult result = createLibrarian(librarianSession, "librarian2@example.com", "secret123");

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("email").isEqualTo("librarian2@example.com");
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_LIBRARIAN");
        assertThat(result).bodyJson().doesNotHavePath("password");

        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=librarian2@example.com&password=secret123")
                .exchange();
        assertThat(loginResult).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/admin");
    }

    @Test
    void testCreateDuplicateLibrarian() {
        MockHttpSession librarianSession = librarianSession();

        MvcTestResult result = createLibrarian(librarianSession, librarianEmail, "secret123");

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.USER_EMAIL_DUPLICATE + librarianEmail);
    }

    @Test
    void testCreateLibrarianTooLongPassword() {
        MockHttpSession librarianSession = librarianSession();

        MvcTestResult result = createLibrarian(librarianSession, "librarian2@example.com", "ą".repeat(40));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.USER_PASSWORD_TOO_LONG);
    }

    @Test
    void testCustomerCannotCreateLibrarian() {
        MockHttpSession customerSession = customerAccount("joe@example.com", "Joe", "Mama").session();

        MvcTestResult result = createLibrarian(customerSession, "evil@example.com", "secret123");

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo("Access Denied");
    }

    @Test
    void testAnonymousCannotCreateLibrarian() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/librarians")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "evil@example.com", "password": "secret123"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.SECURITY_LOGIN_REQUIRED);
        assertThat(userRepository.findByEmail("evil@example.com")).isEmpty();
    }

    @Test
    void testAdminLoginFailure() {
        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=baduser@example.com&password=badpass")
                .exchange();

        assertThat(loginResult)
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login?error=true");
    }

    @Test
    void testCustomerLoginFailure() {
        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("email=baduser@example.com&password=badpass")
                .exchange();

        assertThat(loginResult).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(loginResult).bodyJson().extractingPath("detail").isEqualTo(Messages.SECURITY_LOGIN_FAILURE);
    }

    @Test
    void testApiLogout() {
        MockHttpSession session = customerAccount("joe@example.com", "Joe", "Mama").session();

        MvcTestResult result = mockMvcTester.post().uri("/api/logout").session(session).exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(result.getResponse().getContentAsByteArray()).isEmpty();
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void testStaffLogout() {
        MockHttpSession session = librarianSession();

        MvcTestResult result = mockMvcTester.post().uri("/admin/logout").session(session).exchange();

        assertThat(result).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/login");
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void testAnonymousPageStillRedirectsToLogin() {
        assertThat(mockMvcTester.get().uri("/").accept(MediaType.TEXT_HTML))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/login");
    }

    @Test
    void testAnonymousCanOpenApiDocs() {
        assertThat(mockMvcTester.get().uri("/swagger-ui/index.html").accept(MediaType.TEXT_HTML)).hasStatusOk();
        assertThat(mockMvcTester.get().uri("/swagger-ui/swagger-ui-bundle.js")).hasStatusOk();
        assertThat(mockMvcTester.get().uri("/v3/api-docs/swagger-config")).hasStatusOk();
        assertThat(mockMvcTester.get().uri("/swagger-ui.html").accept(MediaType.TEXT_HTML))
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/swagger-ui/index.html");
    }

    @Test
    void testAnonymousApiCallFromBrowserIsNotRedirected() {
        MvcTestResult result = mockMvcTester.get().uri("/api/admin/customers").accept(MediaType.TEXT_HTML).exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.SECURITY_LOGIN_REQUIRED);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testWhoAmI() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/whoami")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("email").isEqualTo("user");
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_LIBRARIAN");
    }

    @Test
    void testWhoAmIAfterLogin() {
        MockHttpSession session = customerAccount("joe@example.com", "Joe", "Mama").session();

        MvcTestResult result = mockMvcTester.get().uri("/api/whoami").session(session).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("email").isEqualTo("joe@example.com");
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void testWhoAmIAnonymous() {
        MvcTestResult result = mockMvcTester.get().uri("/api/whoami").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().doesNotHavePath("email");
        assertThat(result).bodyJson().doesNotHavePath("role");
    }

    private MvcTestResult registerNewCustomer(String email, String password) {
        MockHttpSession librarianSession = librarianSession();
        String code = activationCode(librarianSession, createCustomer(librarianSession, "Joe", "Mama"));
        return register(code, email, password);
    }

    private MvcTestResult registerJson(String json) {
        return mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult createLibrarian(MockHttpSession session, String email, String password) {
        return mockMvcTester.post()
                .uri("/api/admin/librarians")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password))
                .exchange();
    }

    private static MockHttpSession sessionOf(MvcTestResult loginResult) {
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
