package librarymanagement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CopyControllerTest {
    private static final String INVALID_STATUS_COPY_JSON = """
            {
                "book": {"isbn": "9781234567890"},
                "status": "INVALID_STATUS"
            }
            """;
    private static final String INVALID_BOOK_COPY_JSON = """
            {
                "book": {"isbn": "9999999999999"},
                "status": "AVAILABLE"
            }
            """;
    @Autowired
    private MockMvc mockMvc;
    private MockMvcTester mockMvcTester;

    @PostConstruct
    void setUp() {
        mockMvcTester = MockMvcTester.create(mockMvc);
    }

    private String createCopyJson(String isbn, String status) {
        return """
                {
                    "book": {"isbn": "%s"},
                    "status": "%s"
                }
                """.formatted(isbn, status);
    }

    private MvcResult addBook(BookTestData.BookData bookData) {
        MvcResult addResult = null;
        try {
            addResult = mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookData.JSON))
                    .andExpect(status().isCreated())
                    .andReturn();
        } catch (Exception e) {
            throw new RuntimeException("Failed to add book: " + bookData.ISBN, e);
        }
        return addResult;
    }

    @Test
    void testGetAllCopies() {
        assertThat(mockMvcTester.get().uri("/api/copies")).hasStatus(HttpStatus.OK);
    }

    @Test
    void testAddCopy() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void testAddInvalidCopyInvalidStatus() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Attempt to add a copy with an invalid status
        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_STATUS_COPY_JSON)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("error")
                .isEqualTo("Invalid status. Must be one of: AVAILABLE, RESERVED, BORROWED, LOST");
    }

    @Test
    void testAddInvalidCopyInvalidBook() {
        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(INVALID_BOOK_COPY_JSON)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(testResult).bodyJson().extractingPath("error")
                .isEqualTo("Book not found with ISBN: 9999999999999");
    }

    @Test
    void testSearchCopies() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        // Search for copies by ISBN
        MvcTestResult testResult = mockMvcTester.get()
                .uri("/api/copies/search?isbn=" + bookData.ISBN)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("totalElements").isEqualTo(1);
    }

    @Test
    void testDeleteCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Delete the copy
        MvcTestResult deleteResult = mockMvcTester.delete().uri("/api/copies/" + copyId).exchange();

        assertThat(deleteResult).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void testBorrowCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Borrow the copy
        MvcTestResult borrowResult = mockMvcTester.put().uri("/api/copies/" + copyId + "/borrow").exchange();

        assertThat(borrowResult).hasStatus(HttpStatus.OK);
    }
    // todo test status transitions: return, lost, reserve, undo-reserve
}
