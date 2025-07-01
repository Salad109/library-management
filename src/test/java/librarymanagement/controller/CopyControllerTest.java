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

    // STATUS TRANSITION TESTS

    @Test
    void testBorrowAvailableCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add an available copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the available copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Borrow the available copy
        MvcTestResult borrowResult = mockMvcTester.put().uri("/api/copies/" + copyId + "/borrow").exchange();

        assertThat(borrowResult).hasStatus(HttpStatus.OK);
        assertThat(borrowResult).bodyJson().extractingPath("status").isEqualTo("BORROWED");
    }

    @Test
    void borrowUnavailableCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a reserved copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "LOST");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the reserved copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Attempt to borrow the reserved copy
        MvcTestResult borrowResult = mockMvcTester.put().uri("/api/copies/" + copyId + "/borrow").exchange();

        assertThat(borrowResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(borrowResult).bodyJson().extractingPath("error")
                .isEqualTo("Copy is not available for borrowing. Current status: LOST");
    }

    @Test
    void testMarkCopyAsLost() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add an available copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the available copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Mark the copy as lost
        MvcTestResult lostResult = mockMvcTester.put().uri("/api/copies/" + copyId + "/lost").exchange();

        assertThat(lostResult).hasStatus(HttpStatus.OK);
        assertThat(lostResult).bodyJson().extractingPath("status").isEqualTo("LOST");
    }

    @Test
    void testReserveAvailableCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add an available copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the available copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Reserve the available copy
        MvcTestResult reserveResult = mockMvcTester.put().uri("/api/copies/" + copyId + "/reserve").exchange();

        assertThat(reserveResult).hasStatus(HttpStatus.OK);
        assertThat(reserveResult).bodyJson().extractingPath("status").isEqualTo("RESERVED");
    }

    @Test
    void testReserveUnavailableCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a lost copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "LOST");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the lost copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Attempt to reserve the lost copy
        MvcTestResult reserveResult = mockMvcTester.put().uri("/api/copies/" + copyId + "/reserve").exchange();
        assertThat(reserveResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(reserveResult).bodyJson().extractingPath("error")
                .isEqualTo("Copy is not available for reservation. Current status: LOST");
    }

    @Test
    void testUndoReserveReservedCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a reserved copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "RESERVED");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the reserved copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Undo reserve on the reserved copy
        MvcTestResult undoReserveResult = mockMvcTester.put().uri("/api/copies/" + copyId + "/undo-reserve").exchange();

        assertThat(undoReserveResult).hasStatus(HttpStatus.OK);
        assertThat(undoReserveResult).bodyJson().extractingPath("status").isEqualTo("AVAILABLE");
    }

    @Test
    void testUndoReserveUnreservedCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add an available copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcResult createResult = mockMvc.perform(post("/api/copies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(copyJson))
                .andExpect(status().isCreated())
                .andReturn();

        // Extract the available copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Attempt to undo reserve on the available copy
        MvcTestResult undoReserveResult = mockMvcTester.put().uri("/api/copies/" + copyId + "/undo-reserve").exchange();

        assertThat(undoReserveResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(undoReserveResult).bodyJson().extractingPath("error")
                .isEqualTo("Copy is not currently reserved. Current status: AVAILABLE");
    }
}