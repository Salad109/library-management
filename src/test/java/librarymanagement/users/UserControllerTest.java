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
        String customerJson = """
                {
                    "username": "customer1",
                    "password": "customer123",
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "joe@example.com"
                }
                """;

        MvcTestResult result = register(customerJson);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");
        assertThat(result).bodyJson().extractingPath("customer.firstName").isEqualTo("Joe");
        assertThat(result).bodyJson().doesNotHavePath("password");
    }

    @Test
    void testRegisterIgnoresRequestedRole() {
        String customerJson = """
                {
                    "username": "evil",
                    "password": "password123",
                    "role": "ROLE_LIBRARIAN",
                    "firstName": "Joe",
                    "lastName": "Evil"
                }
                """;

        MvcTestResult result = register(customerJson);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");

        MvcTestResult staffLogin = mockMvcTester.post()
                .uri("/admin/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=evil&password=password123")
                .exchange();
        assertThat(staffLogin).hasStatus(HttpStatus.FOUND).hasRedirectedUrl("/login?error=true");
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
        String customerJson = """
                {
                    "username": "goober",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;
        assertThat(register(customerJson)).hasStatus(HttpStatus.CREATED);

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
    void testRegisterCustomerMissingFields() {
        String invalidCustomerJson = """
                {
                    "username": "goober jr.",
                    "password": "goober123"
                }
                """;

        MvcTestResult result = register(invalidCustomerJson);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("firstName").isEqualTo(Messages.CUSTOMER_FIRSTNAME_VALIDATION_MESSAGE);
        assertThat(result).bodyJson().extractingPath("lastName").isEqualTo(Messages.CUSTOMER_LASTNAME_VALIDATION_MESSAGE);
    }

    @Test
    void testRegisterCustomerInvalidEmail() {
        String invalidEmailJson = """
                {
                    "username": "goober iii",
                    "password": "goober123",
                    "firstName": "Goober",
                    "lastName": "III",
                    "email": "not-an-email"
                }
                """;

        MvcTestResult result = register(invalidEmailJson);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("email")
                .isEqualTo(Messages.CUSTOMER_EMAIL_VALIDATION_MESSAGE);
    }

    @Test
    void testRegisterTooLongUsername() {
        String userJson = """
                {
                    "username": "%s",
                    "password": "secret123",
                    "firstName": "Joe",
                    "lastName": "Mama"
                }
                """.formatted("a".repeat(51));

        MvcTestResult result = register(userJson);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("username")
                .isEqualTo(Messages.USER_USERNAME_LENGTH_VALIDATION_MESSAGE);
    }

    @Test
    void testRegisterTooLongPassword() {
        // 40 characters, but 80 bytes in UTF-8
        String userJson = """
                {
                    "username": "customer1",
                    "password": "%s",
                    "firstName": "Joe",
                    "lastName": "Mama"
                }
                """.formatted("ą".repeat(40));

        MvcTestResult result = register(userJson);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.USER_PASSWORD_TOO_LONG);
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
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(Messages.USER_DUPLICATE + librarianUsername);
    }

    @Test
    void testCreateLibrarianTooLongPassword() {
        MockHttpSession librarianSession = librarianSession();

        MvcTestResult result = createLibrarian(librarianSession, "librarian2", "ą".repeat(40));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(Messages.USER_PASSWORD_TOO_LONG);
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

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyText()
                .contains("LIBRARIAN");
    }

    private MvcTestResult register(String json) {
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
}
