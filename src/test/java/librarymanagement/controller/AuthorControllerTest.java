package librarymanagement.controller;

import jakarta.transaction.Transactional;
import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthorControllerTest {
    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testGetAllAuthors() {
        assertThat(mockMvcTester.get().uri("/api/authors")).hasStatus(HttpStatus.OK);
    }

    @Test
    void testGetAuthorByName() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        String authorName = bookData.AUTHOR1;

        // Add a book to automatically create the author
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        assertThat(mockMvcTester.get().uri("/api/authors/" + authorName))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("name").isEqualTo(authorName);
    }

    @Test
    void testGetNonExistentAuthorByName() {
        String nonExistentAuthorName = "This Author Does Not Exist";

        assertThat(mockMvcTester.get().uri("/api/authors/" + nonExistentAuthorName))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("error")
                .isEqualTo("Author not found with name: " + nonExistentAuthorName);
    }

    @Test
    void testSearchAuthors() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        String authorName = bookData.AUTHOR1;

        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        // Search by author's exact name
        assertThat(mockMvcTester.get().uri("/api/authors/search?name=" + authorName))
                .hasStatus(HttpStatus.OK)
                .bodyJson().
                extractingPath("totalElements").
                isEqualTo(1);

        // Blank search to get all authors
        assertThat(mockMvcTester.get().uri("/api/authors/search")).hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("totalElements").
                isEqualTo(2);
    }
}
