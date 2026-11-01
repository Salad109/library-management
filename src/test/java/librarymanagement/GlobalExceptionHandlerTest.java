package librarymanagement;

import librarymanagement.constants.Messages;
import librarymanagement.utils.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

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
                .extractingPath("detail")
                .isEqualTo("Failed to read request"); // Spring's detail, not Jackson's parser internals
    }

    @Test
    void testNonNumericPathId() {
        assertThat(mockMvcTester.get().uri("/api/admin/copies/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isNotNull();
    }

    @Test
    void testUnsupportedMethod() {
        assertThat(mockMvcTester.patch().uri("/api/admin/customers"))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void testErrorIsProblemDetail() {
        MvcTestResult result = mockMvcTester.get().uri("/api/admin/customers/999").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("status").isEqualTo(404);
        assertThat(result).bodyJson().extractingPath("title").isEqualTo("Not Found");
        assertThat(result).bodyJson().extractingPath("detail").isEqualTo(Messages.CUSTOMER_NOT_FOUND + 999);
        assertThat(result).bodyJson().extractingPath("instance").isEqualTo("/api/admin/customers/999");
    }
}
