package librarymanagement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import librarymanagement.model.CopyStatus;
import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private void addBook(BookTestData.BookData bookData) {
        try {
            mockMvc.perform(post("/api/books")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bookData.JSON))
                    .andExpect(status().isCreated());
        } catch (Exception e) {
            throw new RuntimeException("Failed to add book: " + bookData.ISBN, e);
        }
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

        // Search for copies by ISBN, status and both
        MvcTestResult testResultSearchIsbn = mockMvcTester.get()
                .uri("/api/copies/search?isbn=" + bookData.ISBN)
                .exchange();
        MvcTestResult testResultSearchStatus = mockMvcTester.get()
                .uri("/api/copies/search?status=AVAILABLE").exchange();
        MvcTestResult testResultSearchBoth = mockMvcTester.get()
                .uri("/api/copies/search?isbn=" + bookData.ISBN + "&status=AVAILABLE")
                .exchange();

        assertThat(testResultSearchIsbn).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("totalElements").isEqualTo(1);
        assertThat(testResultSearchStatus).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("totalElements").isEqualTo(1);
        assertThat(testResultSearchBoth).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("totalElements").isEqualTo(1);
    }

    @Test
    void testInvalidSearchCopies() {
        // Search for copies with an invalid status
        MvcTestResult testResult = mockMvcTester.get()
                .uri("/api/copies/search?status=EATEN")
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("error")
                .isEqualTo("Invalid copy status: EATEN. Valid values are: " + Arrays.toString(CopyStatus.values()));
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

    // STATUS TRANSITION TESTS

    @ParameterizedTest
    @CsvSource({
            "AVAILABLE, borrow, BORROWED",
            "BORROWED, return, AVAILABLE",
            "AVAILABLE, lost, LOST",
            "AVAILABLE, reserve, RESERVED",
            "RESERVED, undo-reserve, AVAILABLE"
    })
    void testValidStateTransitions(String initialStatus, String action, String expectedFinalStatus) throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        addBook(bookData);

        // Create copy with initial status
        String copyJson = createCopyJson(bookData.ISBN, initialStatus);
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

        // Perform state transition
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/copies/" + copyId + "/" + action)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("status").isEqualTo(expectedFinalStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "LOST, borrow, 'Copy is not available for borrowing. Current status: LOST'",
            "AVAILABLE, return, 'Copy is not currently borrowed. Current status: AVAILABLE'",
            "LOST, reserve, 'Copy is not available for reservation. Current status: LOST'",
            "AVAILABLE, undo-reserve, 'Copy is not currently reserved. Current status: AVAILABLE'"
    })
    void testInvalidStateTransitions(String initialStatus, String action, String expectedError) throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        addBook(bookData);

        // Create copy with initial status
        String copyJson = createCopyJson(bookData.ISBN, initialStatus);
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

        // Attempt invalid state transition
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/copies/" + copyId + "/" + action)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(expectedError);
    }

    @Test
    void testStateTransitionNonexistingCopy() {
        List<String> transitions = new ArrayList<>(5);
        transitions.add("borrow");
        transitions.add("return");
        transitions.add("lost");
        transitions.add("reserve");
        transitions.add("undo-reserve");

        for (String transition : transitions) {
            assertThat(mockMvcTester.put().uri("/api/copies/999/" + transition).exchange())
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("error")
                    .isEqualTo("Copy not found with ID: 999");
        }
    }
}