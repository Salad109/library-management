package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.utils.BookTestData;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.TestISBNGenerator;
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
@WithMockUser(roles = "LIBRARIAN")
class AdminBookControllerTest {

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
    void testUpdateBook() {
        // Create a book to update
        assertThat(DataBuilder.createTestBook(mockMvcTester,
                BookTestData.TestBook1.ISBN,
                BookTestData.TestBook1.TITLE,
                BookTestData.TestBook1.AUTHOR_NAME)
        ).hasStatus(HttpStatus.CREATED);

        // Verify initial state
        assertThat(mockMvcTester.get().uri("/api/books/" + BookTestData.TestBook1.ISBN))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("title")
                .isEqualTo(BookTestData.TestBook1.TITLE);

        // Update the book
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/books/" + BookTestData.TestBook1.ISBN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook2.JSON)
                .exchange();

        // Verify the update
        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("title").isEqualTo(BookTestData.TestBook2.TITLE);
        assertThat(result).bodyJson().extractingPath("publicationYear").isEqualTo(BookTestData.TestBook2.PUBLICATION_YEAR);
        assertThat(result).bodyJson().extractingPath("authors[0].name").isEqualTo(BookTestData.TestBook2.AUTHOR_NAME);
    }

    @Test
    void testUpdateNonExistentBook() {
        String isbn = TestISBNGenerator.next(); // Nonexistent ISBN
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/admin/books/" + isbn)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.TestBook2.JSON)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.BOOK_NOT_FOUND + isbn);
    }

    @Test
    void testDeleteBook() {
        // Create a book to delete
        assertThat(DataBuilder.createTestBook(mockMvcTester,
                BookTestData.TestBook1.ISBN,
                BookTestData.TestBook1.TITLE,
                BookTestData.TestBook1.AUTHOR_NAME)
        ).hasStatus(HttpStatus.CREATED);

        // Verify the book exists
        assertThat(mockMvcTester.get().uri("/api/books/" + BookTestData.TestBook1.ISBN))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("title")
                .isEqualTo(BookTestData.TestBook1.TITLE);

        // Delete the book
        assertThat(mockMvcTester.delete().uri("/api/admin/books/" + BookTestData.TestBook1.ISBN))
                .hasStatus(HttpStatus.NO_CONTENT);

        // Verify the book no longer exists
        assertThat(mockMvcTester.get().uri("/api/books/" + BookTestData.TestBook1.ISBN))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void testDeleteNonExistentBook() {
        String isbn = TestISBNGenerator.next(); // Nonexistent ISBN

        assertThat(mockMvcTester.delete().uri("/api/admin/books/" + isbn))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.BOOK_NOT_FOUND + isbn);
    }
}
