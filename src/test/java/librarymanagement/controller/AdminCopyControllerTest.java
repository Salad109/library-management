package librarymanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.utils.ControllerTestUtils;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.TestISBNGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@WithMockUser(roles = "LIBRARIAN")
class AdminCopyControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testCreateCopies() {
        String isbn = "123456777X";
        // Create the book first
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);

        String requestJson = """
                {
                    "bookIsbn": "%s",
                    "quantity": 5
                }
                """.formatted(isbn);

        assertThat(mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).hasStatus(HttpStatus.CREATED);
    }

    @ParameterizedTest
    @CsvSource({
            "'9781234567890', 9999999, quantity, " + Messages.COPY_MAXIMUM_QUANTITY_VALIDATION_MESSAGE,
            "'123456789X', 0, quantity, " + Messages.COPY_MINIMUM_QUANTITY_VALIDATION_MESSAGE,
            "'invalid-isbn',, bookIsbn, " + Messages.BOOK_ISBN_VALIDATION_MESSAGE
    })
    void testCreateCopiesInvalidParameters(String isbn, Integer quantity, String errorField, String expectedMessage)
            throws Exception {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("bookIsbn", isbn);
        if (quantity != null) {
            jsonMap.put("quantity", quantity);
        }
        String jsonString = new ObjectMapper().writeValueAsString(jsonMap);

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/admin/copies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonString)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath(errorField)
                .isEqualTo(expectedMessage);
    }


    @Test
    void testGetCopyById() throws Exception {
        String isbn = TestISBNGenerator.next();
        // Create the book and a copy first
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult copyCreationResult = DataBuilder.createTestCopy(mockMvcTester, isbn, 1);
        assertThat(copyCreationResult).hasStatus(HttpStatus.CREATED);
        int copyId = ControllerTestUtils.extractIdFromResponseArray(copyCreationResult);

        assertThat(mockMvcTester.get().uri("/api/admin/copies/" + copyId))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("id")
                .isEqualTo(copyId);
    }

    @Test
    void testGetNonexistentCopyById() {
        int copyId = 9999; // Nonexistent copy ID

        assertThat(mockMvcTester.get().uri("/api/admin/copies/" + copyId))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo(Messages.COPY_NOT_FOUND + copyId);
    }

    @Test
    void testGetCopiesByBookIsbn() {
        String isbn = TestISBNGenerator.next();
        // Create the book and some copies first
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCopy(mockMvcTester, isbn, 3))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.get().uri("/api/admin/copies/book/" + isbn))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements").isEqualTo(3);
    }

    @Test
    void testGetAllCopies() {
        assertThat(mockMvcTester.get().uri("/api/admin/copies"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page").isNotNull();
    }
}
