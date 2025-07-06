package librarymanagement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

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

    private Long addCustomer(String firstName, String lastName) throws Exception {
        String customerJson = """
                {
                    "firstName": "%s",
                    "lastName": "%s"
                }
                """.formatted(firstName, lastName);

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(customerJson)
                .exchange();

        return Long.parseLong(extractIdFromResponse(result));
    }

    public String extractIdFromResponse(MvcTestResult result) throws Exception {
        String responseBody = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("id").asText();
    }

    @Test
    void testGetAllCopies() {
        assertThat(mockMvcTester.get().uri("/api/copies")).hasStatus(HttpStatus.OK);
    }

    @Test
    void testGetCopyById() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String copyId = extractIdFromResponse(createResult);

        // Get the copy by ID
        MvcTestResult testResult = mockMvcTester.get()
                .uri("/api/copies/" + copyId)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.OK);
    }

    @Test
    void testGetNonexistentCopyById() {
        MvcTestResult testResult = mockMvcTester.get()
                .uri("/api/copies/999")
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("error")
                .isEqualTo("Copy not found with ID: 999");
    }

    @Test
    void testGetCopyCountByBookIsbnAndStatus() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        for (int i = 0; i < 5; i++) {
            // Create multiple copies for the same book
            MvcTestResult createResult = mockMvcTester.post()
                    .uri("/api/copies")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(copyJson)
                    .exchange();

            assertThat(createResult).hasStatus(HttpStatus.CREATED);
        }

        // Get the count of available copies for the book
        MvcTestResult testResult = mockMvcTester.get()
                .uri("/api/copies/book/" + bookData.ISBN + "/count")
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.OK)
                .bodyJson().isEqualTo("5");
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
    void testAddCopyNonexistentBook() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");

        // Attempt to add a copy of a non-existing book
        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("error")
                .isEqualTo("Book not found with ISBN: " + bookData.ISBN);
    }

    @Test
    void testAddCopyNullBook() {
        String copyJson = """
                {
                    "status": "AVAILABLE"
                }
                """;

        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();
        assertThat(testResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Copy must be associated with an existing book");
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
    void testDeleteCopy() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        addBook(bookData);

        // Add a copy of that book
        String copyJson = createCopyJson(bookData.ISBN, "AVAILABLE");
        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String copyId = extractIdFromResponse(createResult);

        // Delete the copy
        MvcTestResult deleteResult = mockMvcTester.delete().uri("/api/copies/" + copyId).exchange();

        assertThat(deleteResult).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void testDeleteNonexistentCopy() {
        MvcTestResult deleteResult = mockMvcTester.delete().uri("/api/copies/999").exchange();

        assertThat(deleteResult).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("error")
                .isEqualTo("Copy not found with ID: 999");
    }

    // STATUS TRANSITION TESTS

    @ParameterizedTest
    @CsvSource({
            "BORROWED, return, AVAILABLE",
            "AVAILABLE, lost, LOST",
            "RESERVED, undo-reserve, AVAILABLE"
    })
    void testValidStateTransitions(String initialStatus, String action, String expectedFinalStatus) throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        addBook(bookData);

        Long customerId = addCustomer("Joe", "Mama");

        // Create copy with initial status
        String copyJson = createCopyJson(bookData.ISBN, initialStatus);
        MvcTestResult createCopyResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        assertThat(createCopyResult).hasStatus(HttpStatus.CREATED);

        // Extract the copy ID from the response
        String copyId = extractIdFromResponse(createCopyResult);

        // Perform state transition
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/copies/" + copyId + "/" + action + "/" + customerId)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("status").isEqualTo(expectedFinalStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "AVAILABLE, return, 'Copy is not currently borrowed. Current status: AVAILABLE'",
            "AVAILABLE, undo-reserve, 'Copy is not currently reserved. Current status: AVAILABLE'"
    })
    void testInvalidStateTransitions(String initialStatus, String action, String expectedError) throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        addBook(bookData);

        Long customerId = addCustomer("Jane", "Mama");

        // Create copy with initial status
        String copyJson = createCopyJson(bookData.ISBN, initialStatus);
        MvcTestResult createResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String copyId = extractIdFromResponse(createResult);

        // Attempt invalid state transition
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/copies/" + copyId + "/" + action + "/" + customerId)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("error").isEqualTo(expectedError);
    }

    @ParameterizedTest
    @CsvSource({
            "BORROWED, return",
            "AVAILABLE, lost",
            "RESERVED, undo-reserve"
    })
    void testStateTransitionsInvalidCustomer(String initialState, String action) throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        addBook(bookData);

        // Add customers
        Long oldCustomerId = addCustomer("Joe", "Mama");
        Long newCustomerId = addCustomer("Jane", "Mama");

        // Create copy with initial status
        String copyJson = """
                {
                    "book": {"isbn": "%s"},
                    "status": "%s",
                    "customer": {"id": %d}
                }
                """.formatted(bookData.ISBN, initialState, oldCustomerId);
        MvcTestResult createCopyResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyJson)
                .exchange();

        assertThat(createCopyResult).hasStatus(HttpStatus.CREATED);

        String copyId = extractIdFromResponse(createCopyResult);

        // Attempt state transition with a different customer
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/copies/" + copyId + "/" + action + "/" + newCustomerId)
                .exchange();
        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Copy is not currently used by the specified customer. Current customer: " + oldCustomerId);
    }

    @Test
    void testStateTransitionNonexistentCopy() throws Exception {
        Long customerId = addCustomer("The", "Goober");

        // Test CopyController transitions
        List<String> copyTransitions = List.of("return", "lost", "undo-reserve");

        for (String transition : copyTransitions) {
            assertThat(mockMvcTester.put().uri("/api/copies/999/" + transition + "/" + customerId).exchange())
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("error")
                    .isEqualTo("Copy not found with ID: 999");
        }

        // Test CustomerController transitions
        List<String> customerOperations = List.of("borrow", "reserve");

        for (String operation : customerOperations) {
            assertThat(mockMvcTester.post().uri("/api/customers/" + customerId + "/" + operation + "/9781234567890").exchange())
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("error")
                    .isEqualTo("No available copies found for book with ISBN: 9781234567890");
        }
    }
}