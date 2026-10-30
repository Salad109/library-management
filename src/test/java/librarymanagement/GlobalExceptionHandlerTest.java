package librarymanagement;

import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(roles = "LIBRARIAN")
class GlobalExceptionHandlerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testMalformedJson() {
        assertThat(mockMvcTester.post()
                .uri("/api/admin/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ not json"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Failed to read request"); // Spring's detail, not Jackson's parser internals
    }

    @Test
    void testNonNumericPathId() {
        assertThat(mockMvcTester.get().uri("/api/admin/copies/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("error")
                .isNotNull();
    }

    @Test
    void testUnsupportedMethod() {
        assertThat(mockMvcTester.patch().uri("/api/admin/customers"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
    }
}
