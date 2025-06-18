package librarymanagement.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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

        var content = mockMvcTester.post().uri("/api/books")
                .contentType("application/json")
                .content(bookJson);

        assertThat(content).hasStatusOk();
        assertThat(content).bodyJson()
                .extractingPath("title")
                .isEqualTo("Test Book 3");
        assertThat(content).bodyJson()
                .extractingPath("author")
                .isEqualTo("Test Author");
        assertThat(content).bodyJson()
                .extractingPath("publicationYear")
                .isEqualTo(2023);
        assertThat(content).bodyJson()
                .extractingPath("isbn")
                .isEqualTo("9781234567890");
    }
}