package librarymanagement.copies;

import librarymanagement.constants.Messages;
import librarymanagement.utils.ControllerTestUtils;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.IntegrationTest;
import librarymanagement.utils.TestISBNGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(roles = "LIBRARIAN")
class AdminCopyControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testCreateCopies() {
        String isbn = "123456777X";
        // Create the book first
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);

        String requestJson = """
                {
                    "bookIsbn": "%s",
                    "quantity": 5
                }
                """.formatted(isbn);

        assertThat(mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).hasStatus(HttpStatus.CREATED);
    }

    @ParameterizedTest
    @CsvSource({
            "'9781234567890', 9999999, quantity, " + Messages.COPY_MAXIMUM_QUANTITY_VALIDATION_MESSAGE,
            "'123456789X', 0, quantity, " + Messages.COPY_MINIMUM_QUANTITY_VALIDATION_MESSAGE,
            "'invalid-isbn',, bookIsbn, " + Messages.BOOK_ISBN_VALIDATION_MESSAGE
    })
    void testCreateCopiesInvalidParameters(String isbn, Integer quantity, String errorField, String expectedMessage) {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("bookIsbn", isbn);
        if (quantity != null) {
            jsonMap.put("quantity", quantity);
        }
        String jsonString = new ObjectMapper().writeValueAsString(jsonMap);

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonString)
                .exchange();

        assertFieldError(result, errorField, expectedMessage);
    }

    @Test
    void testCreateCopiesWithoutIsbn() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
                .exchange();

        assertFieldError(result, "bookIsbn", Messages.BOOK_NULL_ISBN);
    }


    @Test
    void testGetCopyById() throws Exception {
        String isbn = TestISBNGenerator.next();
        // Create the book and a copy first
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);
        int copyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        assertThat(mockMvcTester.get().uri("/api/admin/copies/" + copyId))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("id")
                .isEqualTo(copyId);
    }

    @Test
    void testGetNonexistentCopyById() {
        int copyId = 9999; // Nonexistent copy ID

        assertThat(mockMvcTester.get().uri("/api/admin/copies/" + copyId))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_NOT_FOUND + copyId);
    }

    @Test
    void testGetCopiesByBookIsbn() {
        String isbn = TestISBNGenerator.next();
        // Create the book and some copies first
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCopy(mockMvcTester, isbn, 3))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.get().uri("/api/admin/copies?isbn={isbn}", isbn))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements").isEqualTo(3);
    }

    @Test
    void testGetAllCopies() {
        assertThat(mockMvcTester.get().uri("/api/admin/copies"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page").isNotNull();
    }

    @Test
    void testFilterCopies() throws Exception {
        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Filtered Title", "Some Author"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestBook(mockMvcTester, TestISBNGenerator.next(), "Other Book", "Some Author"))
                .hasStatus(HttpStatus.CREATED);
        int copyId = ControllerTestUtils.extractIdFromResponseArray(DataBuilder.createTestCopy(mockMvcTester, isbn, 2));
        int customerId = ControllerTestUtils.extractIdFromResponse(
                DataBuilder.createTestCustomer(mockMvcTester, "Joe", "Mama"));
        assertThat(mockMvcTester.post()
                .uri("/api/desk/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"copyId": %d, "customerId": %d}
                        """.formatted(copyId, customerId)))
                .hasStatus(HttpStatus.OK);

        assertCopyCount("/api/admin/copies?isbn={q}", isbn, 2);
        assertCopyCount("/api/admin/copies?title={q}", "filtered", 2);
        assertCopyCount("/api/admin/copies?customerId={q}", customerId, 1);
        assertCopyCount("/api/admin/copies?customerLastName={q}", "mam", 1);
        assertCopyCount("/api/admin/copies?title={q}", " ", 2);
    }

    @Test
    void testFilterCopiesRejectsTwoFilters() {
        assertThat(mockMvcTester.get().uri("/api/admin/copies?title=a&customerId=1"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.COPY_SINGLE_FILTER);
    }

    private void assertCopyCount(String uri, Object query, int expected) {
        assertThat(mockMvcTester.get().uri(uri, query))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements")
                .isEqualTo(expected);
    }
}
