package librarymanagement.controller;

import librarymanagement.constants.Messages;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
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
    void getAllBooks() {
        MvcTestResult result = mockMvcTester.get().uri("/api/books").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("totalElements")
                .isEqualTo(3);
    }

    @Test
    void getBook() {
        MvcTestResult result = mockMvcTester.get().uri("/api/books/123456789X").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("isbn")
                .isEqualTo("123456789X");
    }

    @Test
    void getNonExistentBook() {
        String nonExistentIsbn = "987654321X";
        MvcTestResult result = mockMvcTester.get().uri("/api/books/" + nonExistentIsbn).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.BOOK_NOT_FOUND + nonExistentIsbn);
    }

    @Test
    void testGetAvailableCopiesCount() {
        MvcTestResult result = mockMvcTester.get().uri("/api/books/9781234567890/count").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyText()
                .isEqualTo("2");
    }

    @Test
    void searchBook() {
        MvcTestResult result = mockMvcTester.get().uri("/api/books/search?title=1984&authorName=").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("totalElements")
                .isEqualTo(1);
    }
}