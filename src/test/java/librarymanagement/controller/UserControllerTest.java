package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testRegisterLibrarian() {
        String librarianJson = """
                {
                    "username": "librarian1",
                    "password": "secret123",
                    "role": "ROLE_LIBRARIAN"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(librarianJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("username").isEqualTo("librarian1");
        assertThat(result).bodyJson().extractingPath("password").asString().isNotEqualTo("secret123"); // Test hashing
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_LIBRARIAN");
    }

    @Test
    void testRegisterCustomer() {
        String customerJson = """
                {
                    "username": "customer1",
                    "password": "customer123",
                    "role": "ROLE_CUSTOMER",
                    "firstName": "Joe",
                    "lastName": "Mama",
                    "email": "joe@example.com"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("role").isEqualTo("ROLE_CUSTOMER");
        assertThat(result).bodyJson().extractingPath("customer.firstName").isEqualTo("Joe");
    }

    @Test
    void testRegisterInvalidUser() {
        String invalidCustomerJson = """
                {
                    "username": "Goober"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCustomerJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("password")
                .isEqualTo(Messages.USER_PASSWORD_VALIDATION_MESSAGE);
        assertThat(result)
                .bodyJson()
                .extractingPath("role")
                .isEqualTo(Messages.USER_ROLE_VALIDATION_MESSAGE);
    }

    @Test
    void testLoginSuccess() {
        // Register a user
        String librarianJson = """
                {
                    "username": "testuser",
                    "password": "testpass123",
                    "role": "ROLE_LIBRARIAN"
                }
                """;

        mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(librarianJson)
                .exchange()
                .assertThat()
                .hasStatus(HttpStatus.CREATED);

        // Try to log in
        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=testuser&password=testpass123")
                .exchange();

        assertThat(loginResult).hasStatus(HttpStatus.OK);
        assertThat(loginResult).bodyText().isEqualTo(Messages.SECURITY_LOGIN_SUCCESS);
    }

    @Test
    void testRegisterCustomerMissingFields() {
        String invalidCustomerJson = """
                {
                    "username": "goober jr.",
                    "password": "goober123",
                    "role": "ROLE_CUSTOMER"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidCustomerJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.USER_MISSING_CUSTOMER_FIELDS);
    }

    @Test
    void testLoginFailure() {
        MvcTestResult loginResult = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=baduser&password=badpass")
                .exchange();

        assertThat(loginResult).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(loginResult).bodyText().isEqualTo(Messages.SECURITY_LOGIN_FAILURE);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testWhoAmI() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/whoami")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyText().contains("LIBRARIAN");
    }
}