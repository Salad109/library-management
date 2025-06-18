package librarymanagement.controller;

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
public class BookControllerTest {
    @Autowired
    MockMvcTester mockMvcTester;

    @Test
    void testGetAllBooks() {
        assertThat(mockMvcTester.get().uri("/api/books")).hasStatusOk();
    }

    @Test
    void testAddBook() {
        String bookJson = """
                {
                    "title": "Test Book 3",
                    "author": "Test Author",
                    "publicationYear": 2023,
                    "isbn": "9781234567890"
                }
                """;

        assertThat(mockMvcTester.post().uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookJson))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo(bookJson);
    }
}