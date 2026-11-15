package librarymanagement.copies;

import librarymanagement.books.Book;
import librarymanagement.utils.ControllerTestUtils;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.IntegrationTest;
import librarymanagement.utils.TestISBNGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(roles = "LIBRARIAN")
class AdminCopyControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testCreateCopies() {
        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"bookIsbn": "%s", "quantity": 5}
                        """.formatted(isbn))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$").asArray().hasSize(5);
        assertThat(result).bodyJson().extractingPath("[0].status").isEqualTo("AVAILABLE");
        assertThat(result).bodyJson().extractingPath("[0].book.isbn").isEqualTo(isbn);
    }

    @Test
    void testCreateCopiesValidation() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "pl") // messages stay English
                .content("""
                        {"bookIsbn": "invalid-isbn", "quantity": 101}
                        """)
                .exchange();

        assertFieldError(result, "bookIsbn", Book.ISBN_INVALID);
        assertFieldError(result, "quantity", "must be less than or equal to 100");
    }

    @Test
    void testGetCopyById() throws Exception {
        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);
        int copyId = ControllerTestUtils.extractIdFromResponseArray(DataBuilder.createTestCopy(mockMvcTester, isbn, 1));

        assertThat(mockMvcTester.get().uri("/api/admin/copies/" + copyId))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("id")
                .isEqualTo(copyId);
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

        // One copy is checked out, the other has no customer
        assertCopyCount(2, "/api/admin/copies?isbn={isbn}", isbn);
        assertCopyCount(2, "/api/admin/copies?title={title}", "filtered");
        assertCopyCount(1, "/api/admin/copies?customerId={id}", customerId);
        assertCopyCount(1, "/api/admin/copies?customerLastName={name}", "mam");
        assertCopyCount(0, "/api/admin/copies?title={title}&customerLastName={name}", "filtered", "other");
        assertCopyCount(2, "/api/admin/copies?title={title}", " ");
    }

    private void assertCopyCount(int expected, String uri, Object... uriVariables) {
        assertThat(mockMvcTester.get().uri(uri, uriVariables))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements")
                .isEqualTo(expected);
    }
}
