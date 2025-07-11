package librarymanagement.controller;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CopyControllerTest {

    private static final String INVALID_STATUS_COPY_JSON = """
            {
                "book": {"isbn": "9781234567890"},
                "status": "INVALID_STATUS"
            }
            """;

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testGetAllCopies() {
        assertThat(mockMvcTester.get().uri("/api/copies")).hasStatus(HttpStatus.OK);
    }

    @Test
    void testGetCopyById() throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        ControllerTestUtils.addBook(mockMvcTester, bookData);

        // Add a copy of that book
        MvcTestResult createResult = ControllerTestUtils.createCopyAndPost(mockMvcTester, bookData.ISBN, "AVAILABLE");

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String copyId = ControllerTestUtils.extractIdFromResponse(createResult);

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
        ControllerTestUtils.addBook(mockMvcTester, bookData);

        // Create multiple copies for the same book
        for (int i = 0; i < 5; i++) {
            MvcTestResult createResult = ControllerTestUtils.createCopyAndPost(mockMvcTester, bookData.ISBN, "AVAILABLE");

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
        ControllerTestUtils.addBook(mockMvcTester, bookData);

        // Add a copy of that book
        MvcTestResult testResult = ControllerTestUtils.createCopyAndPost(mockMvcTester, bookData.ISBN, "AVAILABLE");

        assertThat(testResult).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void testAddCopyNonexistentBook() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Attempt to add a copy of a non-existing book
        MvcTestResult testResult = ControllerTestUtils.createCopyAndPost(mockMvcTester, bookData.ISBN, "AVAILABLE");

        assertThat(testResult).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("error")
                .isEqualTo("Book not found with ISBN: " + bookData.ISBN);
    }

    @Test
    void testAddCopyValidation() {
        String copyNullBookJson = """
                {
                    "status": "AVAILABLE"
                }
                """;
        String copyNullIsbnJson = """
                {
                    "book": {},
                    "status": "AVAILABLE"
                }
                """;

        MvcTestResult nullBookResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyNullBookJson)
                .exchange();
        MvcTestResult nullIsbnResult = mockMvcTester.post()
                .uri("/api/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(copyNullIsbnJson)
                .exchange();

        assertThat(nullBookResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("book")
                .isEqualTo("Book cannot be null");
        assertThat(nullIsbnResult)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Book ISBN cannot be null");
    }

    @Test
    void testAddInvalidCopyInvalidStatus() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add a book
        ControllerTestUtils.addBook(mockMvcTester, bookData);

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
        ControllerTestUtils.addBook(mockMvcTester, bookData);

        // Add a copy of that book
        MvcTestResult createResult = ControllerTestUtils.createCopyAndPost(mockMvcTester, bookData.ISBN, "AVAILABLE");

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String copyId = ControllerTestUtils.extractIdFromResponse(createResult);

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
    @WithMockUser(roles = "LIBRARIAN")
    @CsvSource({
            "BORROWED, return, AVAILABLE, true",
            "AVAILABLE, lost, LOST, true",
            "RESERVED, undo-reserve, AVAILABLE, true",
            "AVAILABLE, return, 'Copy is not currently borrowed', false",
            "RESERVED, return, 'Copy is not currently borrowed', false"
    })
    void testStateTransitions(String initialStatus, String action, String expectedResponse, boolean shouldSucceed) throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        ControllerTestUtils.addBook(mockMvcTester, bookData);

        // Add a customer
        Long customerId = ControllerTestUtils.addCustomer(mockMvcTester, "Joe", "Mama");

        // Create copy with initial status
        MvcTestResult createResult = ControllerTestUtils.createCopyAndPost(mockMvcTester, bookData.ISBN, initialStatus);

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        // Extract the copy ID from the response
        String copyId = ControllerTestUtils.extractIdFromResponse(createResult);

        // Perform state transition
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/copies/" + copyId + "/" + action + "/" + customerId)
                .exchange();

        if (shouldSucceed) {
            assertThat(result).hasStatus(HttpStatus.OK)
                    .bodyJson().extractingPath("status").isEqualTo(expectedResponse);
        } else {
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("error").asString().contains(expectedResponse);
        }
    }

    @ParameterizedTest
    @WithMockUser(roles = "LIBRARIAN")
    @CsvSource({
            "BORROWED, return",
            "AVAILABLE, lost",
            "RESERVED, undo-reserve"
    })
    void testStateTransitionsInvalidCustomer(String initialState, String action) throws Exception {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        ControllerTestUtils.addBook(mockMvcTester, bookData);

        // Add customers
        Long oldCustomerId = ControllerTestUtils.addCustomer(mockMvcTester, "Joe", "Mama");
        Long newCustomerId = ControllerTestUtils.addCustomer(mockMvcTester, "Jane", "Mama");

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

        String copyId = ControllerTestUtils.extractIdFromResponse(createCopyResult);

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
    @WithMockUser(roles = "LIBRARIAN")
    void testStateTransitionNonexistentCopy() throws Exception {
        Long customerId = ControllerTestUtils.addCustomer(mockMvcTester, "The", "Goober");

        // Test CopyController transitions
        List<String> copyTransitions = List.of("return", "lost", "undo-reserve");

        for (String transition : copyTransitions) {
            assertThat(mockMvcTester.put().uri("/api/copies/999/" + transition + "/" + customerId).exchange())
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .extractingPath("error")
                    .isEqualTo("Copy not found with ID: 999");
        }
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testCheckoutReservedCopy() throws Exception {
        // Add a book
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        ControllerTestUtils.addBook(mockMvcTester, bookData);

        // Create a copy
        MvcTestResult createResult = ControllerTestUtils.createCopyAndPost(mockMvcTester, bookData.ISBN, "AVAILABLE");

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String copyId = ControllerTestUtils.extractIdFromResponse(createResult);

        // Reserve the copy
        Long customerId = ControllerTestUtils.addCustomer(mockMvcTester, "Joe", "Mama");

        MvcTestResult reserveResult = mockMvcTester.post()
                .uri("/api/customers/" + customerId + "/reserve/" + bookData.ISBN)
                .exchange();

        assertThat(reserveResult).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("id").isEqualTo(Integer.parseInt(copyId));

        // Checkout the reserved copy
        MvcTestResult checkoutResult = mockMvcTester.put()
                .uri("/api/copies/" + copyId + "/checkout")
                .exchange();

        assertThat(checkoutResult).hasStatus(HttpStatus.OK);
        assertThat(checkoutResult).bodyJson().extractingPath("status").isEqualTo("BORROWED");
    }

    @Test
    void testCheckoutUnreservedCopy() throws Exception {
        // Add a book
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        ControllerTestUtils.addBook(mockMvcTester, bookData);

        // Create a copy
        MvcTestResult createResult = ControllerTestUtils.createCopyAndPost(mockMvcTester, bookData.ISBN, "AVAILABLE");

        assertThat(createResult).hasStatus(HttpStatus.CREATED);

        String copyId = ControllerTestUtils.extractIdFromResponse(createResult);

        // Attempt to check out the copy without reserving it first
        MvcTestResult checkoutResult = mockMvcTester.put()
                .uri("/api/copies/" + copyId + "/checkout")
                .exchange();

        assertThat(checkoutResult).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("error")
                .isEqualTo("Copy is not currently reserved. Current status: AVAILABLE");
    }
}