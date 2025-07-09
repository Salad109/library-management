package librarymanagement.config;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityConfigurationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testLoginSuccess() throws Exception {
        // Register a user
        String registerJson = """
                {
                    "username": "Joe",
                    "password": "Joe123",
                    "role": "ROLE_LIBRARIAN"
                }
                """;

        mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson)
                .exchange();

        // Log in
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=Joe&password=Joe123")
                .exchange();

        // Assert
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result.getResponse().getContentAsString()).isEqualTo("Login successful!");
    }

    @Test
    void testLoginFailure() throws Exception {
        // Log in as a user that does not exist
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("username=Goober&password=Goober123")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponse().getContentAsString()).isEqualTo("Login failed!");
    }

    @Test
    void testLogoutSuccess() throws Exception {
        MvcTestResult result = mockMvcTester.post().uri("/api/logout").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result.getResponse().getContentAsString()).isEqualTo("Logout successful!");
    }

    // todo test authorization of select endpoints
}