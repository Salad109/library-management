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

        MvcTestResult result = register(code, "customer1", "customer123");

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
                    "username": "evil",
                    "password": "password123",
                    "role": "ROLE_LIBRARIAN"
                }
                """.formatted(code));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");

        MvcTestResult staffLogin = mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=evil&password=password123")
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
                .content("username=" + librarianUsername + "&password=" + librarianPassword)
                .exchange();

        assertThat(loginResult)
                .hasStatus(HttpStatus.FOUND)
                .hasRedirectedUrl("/admin");
    }

    @Test
    void testLoginCustomer() {
        assertThat(registerNewCustomer("goober", "password123")).hasStatus(HttpStatus.CREATED);

        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=goober&password=password123")
                .exchange();

        assertThat(loginResult)
                .hasStatus(HttpStatus.OK)
                .bodyText()
                .contains(Messages.SECURITY_LOGIN_SUCCESS);
    }

    @Test
    void testLibrarianLoginThroughApiLogin() {
        librarianSession();

        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=" + librarianUsername + "&password=" + librarianPassword)
                .exchange();

        assertThat(loginResult).hasStatus(HttpStatus.OK).bodyText().contains(Messages.SECURITY_LOGIN_SUCCESS);
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
        assertFieldError(result, "username", Messages.USER_USERNAME_VALIDATION_MESSAGE);
        assertFieldError(result, "password", Messages.USER_PASSWORD_VALIDATION_MESSAGE);
    }

    @Test
    void testRegisterTooLongUsername() {
        MvcTestResult result = registerNewCustomer("a".repeat(51), "secret123");

        assertFieldError(result, "username", Messages.USER_USERNAME_LENGTH_VALIDATION_MESSAGE);
    }

    @Test
    void testRegisterTooLongPassword() {
        // 40 characters, but 80 bytes in UTF-8
        MvcTestResult result = registerNewCustomer("customer1", "ą".repeat(40));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.USER_PASSWORD_TOO_LONG);
    }

    @Test
    void testRegisterDuplicateUsername() {
        assertThat(registerNewCustomer("customer1", "secret123")).hasStatus(HttpStatus.CREATED);

        MvcTestResult result = registerNewCustomer("customer1", "secret123");

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.USER_DUPLICATE + "customer1");
    }

    @Test
    void testCreateLibrarian() {
        MockHttpSession librarianSession = librarianSession();

        MvcTestResult result = createLibrarian(librarianSession, "librarian2", "secret123");

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("username").isEqualTo("librarian2");
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_LIBRARIAN");
        assertThat(result).bodyJson().doesNotHavePath("password");

        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=librarian2&password=secret123")
                .exchange();
        assertThat(loginResult).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/admin");
    }

    @Test
    void testCreateDuplicateLibrarian() {
        MockHttpSession librarianSession = librarianSession();

        MvcTestResult result = createLibrarian(librarianSession, librarianUsername, "secret123");

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.USER_DUPLICATE + librarianUsername);
    }

    @Test
    void testCreateLibrarianTooLongPassword() {
        MockHttpSession librarianSession = librarianSession();

        MvcTestResult result = createLibrarian(librarianSession, "librarian2", "ą".repeat(40));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.USER_PASSWORD_TOO_LONG);
    }

    @Test
    void testCustomerCannotCreateLibrarian() {
        MockHttpSession customerSession = customerAccount("joe", "Joe", "Mama").session();

        assertThat(createLibrarian(customerSession, "evil", "secret123")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void testAnonymousCannotCreateLibrarian() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/librarians")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "evil", "password": "secret123"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FOUND);
        assertThat(userRepository.findByUsername("evil")).isEmpty();
    }

    @Test
    void testAdminLoginFailure() {
        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=baduser&password=badpass")
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
                .content("username=baduser&password=badpass")
                .exchange();

        assertThat(loginResult)
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyText()
                .contains(Messages.SECURITY_LOGIN_FAILURE);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testWhoAmI() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/whoami")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("username").isEqualTo("user");
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_LIBRARIAN");
    }

    @Test
    void testWhoAmIAfterLogin() {
        MockHttpSession session = customerAccount("joe", "Joe", "Mama").session();

        MvcTestResult result = mockMvcTester.get().uri("/api/whoami").session(session).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("username").isEqualTo("joe");
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void testWhoAmIAnonymous() {
        MvcTestResult result = mockMvcTester.get().uri("/api/whoami").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().doesNotHavePath("username");
        assertThat(result).bodyJson().doesNotHavePath("role");
    }

    private MvcTestResult registerNewCustomer(String username, String password) {
        MockHttpSession librarianSession = librarianSession();
        String code = activationCode(librarianSession, createCustomer(librarianSession, "Joe", "Mama"));
        return register(code, username, password);
    }

    private MvcTestResult registerJson(String json) {
        return mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult createLibrarian(MockHttpSession session, String username, String password) {
        return mockMvcTester.post()
                .uri("/api/admin/librarians")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username": "%s", "password": "%s"}
                        """.formatted(username, password))
                .exchange();
    }

    private static MockHttpSession sessionOf(MvcTestResult loginResult) {
        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
