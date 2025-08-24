package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.utils.ControllerTestUtils;
import librarymanagement.utils.DataBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
@WithMockUser(roles = "LIBRARIAN")
class AdminCopyControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testCreateCopies() {
        String isbn = "123456789X";
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);

        String requestJson = """
                {
                    "bookIsbn": "%s",
                    "quantity": 5
                }
                """.formatted(isbn);

        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);
    }

    @ParameterizedTest
    @CsvSource({
            "'9781234567890', 9999999, quantity, " + Messages.COPY_MAXIMUM_QUANTITY_VALIDATION_MESSAGE,
            "'123456789X', 0, quantity, " + Messages.COPY_MINIMUM_QUANTITY_VALIDATION_MESSAGE,
            "'invalid-isbn',, bookIsbn, " + Messages.BOOK_ISBN_VALIDATION_MESSAGE
    })
    void testCreateCopiesInvalidParameters(String isbn, Integer quantity, String errorField, String expectedMessage) {
        StringBuilder json = new StringBuilder("{\n");
        json.append("    \"bookIsbn\": \"").append(isbn).append("\"");
        if (quantity != null) {
            json.append(",\n    \"quantity\": ").append(quantity);
        }
        json.append("\n}");

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.toString())
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath(errorField).isEqualTo(expectedMessage);
    }

    @Test
    void testGetCopyById() throws Exception {
        String isbn = "9781234567890";
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);
        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);
        int copyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        assertThat(mockMvcTester.get()
                .uri("/api/admin/copies/" + copyId))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void testGetNonexistentCopyById() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/copies/9999")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(Messages.COPY_NOT_FOUND + "9999");
    }

    @Test
    void testGetCopiesByBookIsbn() {
        String isbn = "9781234567890";
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCopy(mockMvcTester, isbn, 3))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/copies/book/" + isbn)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements").isEqualTo(3);
    }

    @Test
    void testGetAllCopies() {
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/admin/copies")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
    }
}
