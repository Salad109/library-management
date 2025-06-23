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
class BookControllerTest {
    @Autowired
    MockMvc mockMvc;

    MockMvcTester mockMvcTester;

    @PostConstruct
    void setUp() {
        mockMvcTester = MockMvcTester.create(mockMvc);
    }

    @Test
    void testGetAllBooks() {
        assertThat(mockMvcTester.get().uri("/api/books")).hasStatus(HttpStatus.OK);
    }

    @Test
    void testAddBook() {
        MvcTestResult testResult = mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook1.JSON).exchange();

        assertThat(testResult).hasStatus(HttpStatus.CREATED).bodyJson().isLenientlyEqualTo(BookTestData.ValidBook1.JSON);
    }

    @Test
    void testAddInvalidBook() {
        MvcTestResult testResult = mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.InvalidBookNoTitleInvalidIsbn.JSON).exchange();

        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST);

        assertThat(testResult).bodyJson().extractingPath("isbn").isEqualTo("ISBN must be 10 digits (last can be X) or 13 digits starting with 978/979");

        assertThat(testResult).bodyJson().extractingPath("title").isEqualTo("Title cannot be blank");
    }

    @Test
    void testAddDuplicateIsbnBook() {
        // Add once
        mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook2.JSON).exchange();

        // Add the same book again
        MvcTestResult testResult = mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook2.JSON).exchange();

        assertThat(testResult).hasStatus(HttpStatus.CONFLICT);
        assertThat(testResult).bodyJson().extractingPath("error").isEqualTo("A book with this ISBN already exists: " + BookTestData.ValidBook2.ISBN);
    }

    @Test
    void testSearchBooks() {
        // Add multiple books to search
        mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook3.JSON).exchange();
        mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook4.JSON).exchange();

        // Search by unique ISBN, expect one result
        MvcTestResult isbnSearchResult = mockMvcTester.get().uri("/api/books/search").param("isbn", BookTestData.ValidBook3.ISBN).param("order", "title").exchange();
        assertThat(isbnSearchResult).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("totalElements").isEqualTo(1);
        assertThat(isbnSearchResult).bodyJson().extractingPath("content[0].isbn").isEqualTo(BookTestData.ValidBook3.ISBN);

        // Search by shared year, expect multiple results
        MvcTestResult yearSearchResult = mockMvcTester.get().uri("/api/books/search").param("publicationYear", BookTestData.ValidBook3.PUBLICATION_YEAR.toString()).exchange();
        assertThat(yearSearchResult).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("totalElements").isEqualTo(2);
        assertThat(yearSearchResult).bodyJson().extractingPath("content[0].isbn").isEqualTo(BookTestData.ValidBook3.ISBN);
        assertThat(yearSearchResult).bodyJson().extractingPath("content[1].isbn").isEqualTo(BookTestData.ValidBook4.ISBN);
    }

    @Test
    void testUpdateBook() throws Exception {
        // Create a book to update
        MvcResult createResult = mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook5.JSON)).andExpect(status().isCreated()).andReturn();

        // Extract book ISBN from response
        String responseContent = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String bookIsbn = jsonNode.get("isbn").asText();

        // Update the book
        MvcTestResult updateResult = mockMvcTester.put().uri("/api/books/" + bookIsbn).contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook5Updated.JSON).exchange();

        // Verify the update was successful
        assertThat(updateResult).hasStatus(HttpStatus.OK).bodyJson().isLenientlyEqualTo(BookTestData.ValidBook5Updated.JSON);
    }

    @Test
    void testUpdateBookChangeIsbn() throws Exception {
        // Create a book to update
        MvcResult createResult = mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook6.JSON)).andExpect(status().isCreated()).andReturn();

        // Extract book ISBN from response
        String responseContent = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String bookIsbn = jsonNode.get("isbn").asText();

        // Try to change the ISBN
        MvcTestResult updateResult = mockMvcTester.put().uri("/api/books/" + bookIsbn).contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook7.JSON).exchange();
        assertThat(updateResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(updateResult).bodyJson().extractingPath("error").isEqualTo("Cannot change ISBN of an existing book");

        // Verify the book is unchanged
        MvcTestResult getResult = mockMvcTester.get().uri("/api/books/" + bookIsbn).exchange();
        assertThat(getResult).hasStatus(HttpStatus.OK).bodyJson().isLenientlyEqualTo(BookTestData.ValidBook6.JSON);
    }

    @Test
    void testUpdateNonExistentBook() {
        MvcTestResult testResult = mockMvcTester.put().uri("/api/books/9999").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook8.JSON).exchange();

        assertThat(testResult).hasStatus(HttpStatus.NOT_FOUND).bodyJson().extractingPath("error").isEqualTo("Book not found with ISBN: 9999");
    }

    @Test
    void testDeleteBook() throws Exception {
        // Create a book to delete
        MvcResult createResult = mockMvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook9.JSON)).andExpect(status().isCreated()).andReturn();

        // Extract book ID from response
        String responseContent = createResult.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseContent);
        String bookIsbn = jsonNode.get("isbn").asText();

        // Delete the book
        assertThat(mockMvcTester.delete().uri("/api/books/" + bookIsbn).contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook10.JSON)).hasStatus(HttpStatus.NO_CONTENT);

        // Verify the book is deleted
        MvcTestResult getResult = mockMvcTester.get().uri("/api/books/" + bookIsbn).exchange();
        assertThat(getResult).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(getResult).bodyJson().extractingPath("error").isEqualTo("Book not found with ISBN: " + bookIsbn);
    }
}