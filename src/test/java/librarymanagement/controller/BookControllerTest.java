package librarymanagement.controller;

import librarymanagement.testdata.BookTestData;
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
        mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook2.JSON).exchange();

        MvcTestResult testResult = mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook2.JSON).exchange();

        assertThat(testResult).hasStatus(HttpStatus.CONFLICT);
        assertThat(testResult).bodyJson().extractingPath("error").isEqualTo("A book with this ISBN already exists: " + BookTestData.ValidBook2.ISBN);
    }

    @Test
    void testSearchBooks() {
        mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook3.JSON).exchange();
        mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook4.JSON).exchange();

        MvcTestResult isbnSearchResult = mockMvcTester.get().uri("/api/books/search").param("isbn", BookTestData.ValidBook3.ISBN).exchange();
        assertThat(isbnSearchResult).hasStatus(HttpStatus.OK).bodyJson().isLenientlyEqualTo("[" + BookTestData.ValidBook3.JSON + "]");

        MvcTestResult titleSearchResult = mockMvcTester.get().uri("/api/books/search").param("publicationYear", BookTestData.ValidBook3.PUBLICATION_YEAR.toString()).exchange();
        assertThat(titleSearchResult).hasStatus(HttpStatus.OK).bodyJson().isLenientlyEqualTo("[" + BookTestData.ValidBook3.JSON + ", " + BookTestData.ValidBook4.JSON + "]");
    }

    @Test
    void testUpdateBook() {
        mockMvcTester.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(BookTestData.ValidBook5.JSON).exchange();

        MvcTestResult updateResult = mockMvcTester.put().uri("/api/books/3").contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.ValidBook6.JSON).exchange();
        assertThat(updateResult).hasStatus(HttpStatus.OK).bodyJson().isLenientlyEqualTo(BookTestData.ValidBook6.JSON);
    }

    @Test
    void testUpdateNonExistentBook() {
        // todo
    }

    @Test
    void testDeleteBook() {
        // todo
    }
}