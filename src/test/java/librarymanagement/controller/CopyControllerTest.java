package librarymanagement.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import librarymanagement.testdata.BookTestData;
import librarymanagement.testdata.CopyTestData;
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
    @Autowired
    private MockMvc mockMvc;

    private MockMvcTester mockMvcTester;

    @PostConstruct
    void setUp() {
        mockMvcTester = MockMvcTester.create(mockMvc);
    }

    @Test
    void testGetAllCopies() {
        assertThat(mockMvcTester.get().uri("/api/copies")).hasStatus(HttpStatus.OK);
    }

    @Test
    void testAddCopy() {
        // Add a book
        mockMvcTester.post().uri("/api/books").contentType("application/json").content(BookTestData.ValidBook1.JSON).exchange();

        // Add a copy of that book
        MvcTestResult testResult = mockMvcTester.post().uri("/api/copies").contentType("application/json").content(CopyTestData.ValidCopy1.JSON).exchange();

        assertThat(testResult).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo(CopyTestData.ValidCopy1.JSON);
    }

    @Test
    void testAddInvalidCopyInvalidStatus() {
        // Add a book
        mockMvcTester.post().uri("/api/books").contentType("application/json").content(BookTestData.ValidBook1.JSON).exchange();

        // Attempt to add a copy with an invalid status
        MvcTestResult testResult = mockMvcTester.post().uri("/api/copies").contentType("application/json").content(CopyTestData.InvalidCopyInvalidStatus.JSON).exchange();

        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("error").isEqualTo("Invalid status. Must be one of: AVAILABLE, RESERVED, BORROWED, LOST");
    }

    @Test
    void testAddInvalidCopyInvalidBook() {
        MvcTestResult testResult = mockMvcTester.post().uri("/api/copies").contentType("application/json").content(CopyTestData.InvalidCopyInvalidBook.JSON).exchange();

        assertThat(testResult).hasStatus(HttpStatus.NOT_FOUND);

        assertThat(testResult).bodyJson().extractingPath("error").isEqualTo("Book not found with ISBN: " + CopyTestData.InvalidCopyInvalidBook.ISBN);
    }

    @Test
    void testSearchCopies() {
        // Add a book
        mockMvcTester.post().uri("/api/books").contentType("application/json").content(BookTestData.ValidBook1.JSON).exchange();

        // Add a copy of that book
        mockMvcTester.post().uri("/api/copies").contentType("application/json").content(CopyTestData.ValidCopy1.JSON).exchange();

        // Search for copies by ISBN
        MvcTestResult testResult = mockMvcTester.get().uri("/api/copies/search?isbn=" + BookTestData.ValidBook1.ISBN).exchange();

        assertThat(testResult).hasStatus(HttpStatus.OK).bodyJson().extractingPath("totalElements").isEqualTo(1);
    }

    @Test
    void testDeleteCopy() throws Exception {
        // Add a book
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.ValidBook1.JSON)
                .exchange();

        // Add a copy of that book
        MvcResult createResult = mockMvc.perform(post("/api/copies").contentType(MediaType.APPLICATION_JSON).content(CopyTestData.ValidCopy1.JSON)).andExpect(status().isCreated()).andReturn();

        // Extract the copy ID from the response
        String responseBody = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String copyId = jsonNode.get("id").asText();

        // Delete the copy
        MvcTestResult deleteResult = mockMvcTester.delete().uri("/api/copies/" + copyId).exchange();

        assertThat(deleteResult).hasStatus(HttpStatus.NO_CONTENT);
    }
    // todo test status transitions
}
