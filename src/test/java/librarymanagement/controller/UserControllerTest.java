package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
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
@AutoConfigureMockMvc(addFilters = false)
@Transactional
public class UserControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testRegister() {
        String customerJson = """
                {
                    "username": "joe.mama",
                    "password": "joe123",
                    "role": "ROLE_CUSTOMER",
                    "firstName": "Joe",
                    "lastName": "Mama"
                }
                """;

        MvcTestResult registerResult = mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        assertThat(registerResult)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("role")
                .isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    void registerInvalidUser() {
        String invalidUserJson = """
                {
                    "username": ""
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUserJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("username")
                .isEqualTo(Messages.USER_USERNAME_VALIDATION_MESSAGE);
        assertThat(result)
                .bodyJson()
                .extractingPath("role")
                .isEqualTo(Messages.USER_ROLE_VALIDATION_MESSAGE);
    }

    @Test
    void testRegisterInvalidPassword() {
        String invalidUserJson = """
                {
                    "username": "testuser",
                    "password": "",
                    "role": "ROLE_CUSTOMER"
                }
                """;

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUserJson)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("password")
                .isEqualTo(Messages.USER_PASSWORD_VALIDATION_MESSAGE);
    }
}
