package librarymanagement.controller;

import librarymanagement.constants.Messages;
import librarymanagement.utils.DataBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetBook() {
        String isbn = "123456789X";
        assertThat(DataBuilder.createTestBook(
                mockMvcTester,
                isbn,
                "Test Book",
                "Test Author")
        ).hasStatus(HttpStatus.CREATED);

        MvcTestResult result = mockMvcTester.get().uri("/api/books/" + isbn).exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("isbn")
                .isEqualTo(isbn);
    }

    @Test
    void testGetNonExistentBook() {
        String isbn = "987654321X"; // Nonexistent ISBN

        assertThat(mockMvcTester.get().uri("/api/books/" + isbn))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.BOOK_NOT_FOUND + isbn);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void searchBook() {
        String isbn = "777888999X";
        String title = "1984";
        // Create the book first
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, title, "George Orwell"))
                .hasStatus(HttpStatus.CREATED);

        // Search by title
        assertThat(mockMvcTester.get().uri("/api/books/search?q=1984"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements")
                .isEqualTo(1);
    }

    @Test
    void testGetAllBooks() {
        assertThat(mockMvcTester.get().uri("/api/books"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("content")
                .isNotNull();
    }
}