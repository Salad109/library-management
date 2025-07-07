package librarymanagement.controller;

import jakarta.transaction.Transactional;
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
@Transactional
class BookControllerTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    void testAddBook() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        MvcTestResult result = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .isLenientlyEqualTo(bookData.JSON);
    }

    @Test
    void testAddInvalidBook() {
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.InvalidBookNoTitleInvalidIsbn.JSON)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson()
                .extractingPath("isbn")
                .isEqualTo("ISBN must be 10 digits (last can be X) or 13 digits starting with 978/979");
        assertThat(result).bodyJson()
                .extractingPath("title")
                .isEqualTo("Title cannot be blank");
    }

    @Test
    void testAddDuplicateIsbnBook() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add once
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        // Add the same book again
        MvcTestResult result = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("A book with this ISBN already exists: " + bookData.ISBN);
    }

    @Test
    void testGetAllBooks() {
        assertThat(mockMvcTester.get().uri("/api/books"))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void testSearchBooksByIsbn() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Add one book
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        // Search by ISBN
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/books/search")
                .param("isbn", bookData.ISBN)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("totalElements")
                .isEqualTo(1);
    }

    @Test
    void testSearchBooksByYear() {
        BookTestData.BookData bookData1 = BookTestData.getNextBookData();
        BookTestData.BookData bookData2 = BookTestData.getNextBookData();

        // Create second book with same year as first
        String bookData2SameYearJson = """
                {
                    "title": "%s",
                    "publicationYear": %d,
                    "isbn": "%s",
                    "authors": [
                        {
                            "name": "%s"
                        },
                        {
                            "name": "%s"
                        }
                    ]
                }
                """.formatted(
                bookData2.TITLE,
                bookData1.PUBLICATION_YEAR,
                bookData2.ISBN,
                bookData2.AUTHOR1,
                bookData2.AUTHOR2
        );

        // Add both books
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData1.JSON)
                .exchange();

        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData2SameYearJson)
                .exchange();

        // Search by year
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/books/search")
                .param("publicationYear", bookData1.PUBLICATION_YEAR.toString())
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("totalElements")
                .isEqualTo(2);
    }

    @Test
    void testUpdateBook() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Create a book to update
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        // Create updated version
        String updatedBookJson = """
                {
                    "title": "%s",
                    "publicationYear": %d,
                    "isbn": "%s",
                    "authors": [
                        {
                            "name": "%s"
                        },
                        {
                            "name": "%s"
                        }
                    ]
                }
                """.formatted(
                bookData.TITLE + " Updated",
                bookData.PUBLICATION_YEAR + 1,
                bookData.ISBN,
                bookData.AUTHOR1 + " Updated",
                bookData.AUTHOR2 + " Updated"
        );

        // Update the book
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/books/" + bookData.ISBN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedBookJson)
                .exchange();

        // Assert
        assertThat(result)
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .isLenientlyEqualTo(updatedBookJson);
    }

    @Test
    void testUpdateBookChangeIsbn() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        BookTestData.BookData differentBookData = BookTestData.getNextBookData();

        // Create a book to update
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        String invalidUpdateJson = """
                {
                    "title": "%s",
                    "isbn": "%s",
                    "authors": [
                        {
                            "name": "%s"
                        }
                    ]
                }
                """.formatted(
                bookData.TITLE,
                differentBookData.ISBN,
                bookData.AUTHOR1
        );

        // Try to change the ISBN
        MvcTestResult result = mockMvcTester.put()
                .uri("/api/books/" + bookData.ISBN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUpdateJson)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Cannot change ISBN of an existing book");
    }

    @Test
    void testUpdateNonExistentBook() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        MvcTestResult result = mockMvcTester.put()
                .uri("/api/books/" + bookData.ISBN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Book not found with ISBN: " + bookData.ISBN);
    }

    @Test
    void testDeleteBook() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        // Create a book to delete
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bookData.JSON)
                .exchange();

        // Delete the book
        assertThat(mockMvcTester.delete().uri("/api/books/" + bookData.ISBN))
                .hasStatus(HttpStatus.NO_CONTENT);

        // Verify the book is deleted
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/books/" + bookData.ISBN)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Book not found with ISBN: " + bookData.ISBN);
    }

    @Test
    void testDeleteNonExistentBook() {
        BookTestData.BookData bookData = BookTestData.getNextBookData();

        MvcTestResult result = mockMvcTester.delete()
                .uri("/api/books/" + bookData.ISBN)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("error")
                .isEqualTo("Book not found with ISBN: " + bookData.ISBN);
    }
}