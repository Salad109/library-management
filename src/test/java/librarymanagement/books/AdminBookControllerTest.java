package librarymanagement.books;

import librarymanagement.constants.Messages;
import librarymanagement.utils.BookTestData;
import librarymanagement.utils.DataBuilder;
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
class AdminBookControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testCreateBook() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook1.JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("title").isEqualTo(BookTestData.TestBook1.TITLE);
        assertThat(result).bodyJson().extractingPath("publicationYear").isEqualTo(BookTestData.TestBook1.PUBLICATION_YEAR);
        assertThat(result).bodyJson().extractingPath("authors[0].name").isEqualTo(BookTestData.TestBook1.AUTHOR_NAME);
    }

    @Test
    void testCreateBookWithExistingAuthor() {
        assertThat(DataBuilder.createTestBook(mockMvcTester,
                BookTestData.TestBook1.ISBN,
                BookTestData.TestBook1.TITLE,
                BookTestData.TestBook1.AUTHOR_NAME)
        ).hasStatus(HttpStatus.CREATED);

        // Second book by the same author reuses the stored author
        MvcTestResult result = DataBuilder.createTestBook(mockMvcTester,
                BookTestData.TestBook2.ISBN,
                BookTestData.TestBook2.TITLE,
                BookTestData.TestBook1.AUTHOR_NAME);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("authors[0].name").isEqualTo(BookTestData.TestBook1.AUTHOR_NAME);
    }

    @Test
    void testCreateBookValidation() {
        MvcTestResult invalidFields = postBook("""
                {"title": "%s", "authorNames": [" ", "%s"]}
                """.formatted("a".repeat(151), "a".repeat(101)));

        assertFieldError(invalidFields, "isbn", "must not be null");
        assertFieldError(invalidFields, "title", "size must be between 0 and 150");
        assertFieldError(invalidFields, "authorNames", "must not be blank");
        assertFieldError(invalidFields, "authorNames", "size must be between 0 and 100");

        MvcTestResult noAuthors = postBook("""
                {"isbn": "%s", "title": "Title"}
                """.formatted(BookTestData.TestBook1.ISBN));

        assertFieldError(noAuthors, "authorNames", "must not be null");
    }

    @Test
    void testUpdateBook() {
        assertThat(DataBuilder.createTestBook(mockMvcTester,
                BookTestData.TestBook1.ISBN,
                BookTestData.TestBook1.TITLE,
                BookTestData.TestBook1.AUTHOR_NAME)
        ).hasStatus(HttpStatus.CREATED);

        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/books/" + BookTestData.TestBook1.ISBN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook2.JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("title").isEqualTo(BookTestData.TestBook2.TITLE);
        assertThat(result).bodyJson().extractingPath("publicationYear").isEqualTo(BookTestData.TestBook2.PUBLICATION_YEAR);
        assertThat(result).bodyJson().extractingPath("authors[0].name").isEqualTo(BookTestData.TestBook2.AUTHOR_NAME);
    }

    @Test
    void testDeleteBook() {
        assertThat(DataBuilder.createTestBook(mockMvcTester,
                BookTestData.TestBook1.ISBN,
                BookTestData.TestBook1.TITLE,
                BookTestData.TestBook1.AUTHOR_NAME)
        ).hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.delete().uri("/api/admin/books/" + BookTestData.TestBook1.ISBN))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(mockMvcTester.get().uri("/api/books/" + BookTestData.TestBook1.ISBN))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testDeleteBookWithCopies() {
        assertThat(DataBuilder.createTestBook(mockMvcTester,
                BookTestData.TestBook1.ISBN,
                BookTestData.TestBook1.TITLE,
                BookTestData.TestBook1.AUTHOR_NAME)
        ).hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCopy(mockMvcTester, BookTestData.TestBook1.ISBN, 1))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.delete().uri("/api/admin/books/" + BookTestData.TestBook1.ISBN))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.BOOK_HAS_COPIES + BookTestData.TestBook1.ISBN);

        assertThat(mockMvcTester.get().uri("/api/books/" + BookTestData.TestBook1.ISBN))
                .hasStatus(HttpStatus.OK);
    }

    private MvcTestResult postBook(String json) {
        return mockMvcTester.post()
                .uri("/api/admin/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }
}
