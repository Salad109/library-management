package librarymanagement.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class BookControllerTest {
    @Autowired
    MockMvcTester mockMvcTester;

    @Test
    void testGetAllBooks() {
        assertThat(mockMvcTester.get().uri("/api/books")).hasStatus(HttpStatus.OK);
    }

    @Test
    void testAddBook() {
        String requestBody = """
                {
                    "title": "Valid Book",
                    "author": "Joe Mama",
                    "publicationYear": 2025,
                    "isbn": "9781234567890"
                }
                """;

        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
                .exchange();

        assertThat(testResult)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo(requestBody);
    }

    @Test
    void testAddInvalidBook() {
        String untitledBook = """
                {
                    "isbn": "no"
                }
                """;

        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(untitledBook)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST);

        assertThat(testResult).bodyJson()
                .extractingPath("isbn")
                .isEqualTo("ISBN must be 10 digits (last can be X) or 13 digits starting with 978/979");

        assertThat(testResult).bodyJson()
                .extractingPath("title")
                .isEqualTo("Title cannot be blank");
    }

    @Test
    void testAddDuplicateIsbnBook() {
        String duplicateIsbnBook = """
                {
                    "title": "Duplicate ISBN Book",
                    "author": "Jane Mama",
                    "publicationYear": 2025,
                    "isbn": "9781234567891"
                }
                """;

        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateIsbnBook)
                .exchange();

        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(duplicateIsbnBook)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.CONFLICT);
        assertThat(testResult).bodyJson()
                .extractingPath("isbn")
                .isEqualTo("A book with this ISBN already exists: 9781234567891");
    }
}