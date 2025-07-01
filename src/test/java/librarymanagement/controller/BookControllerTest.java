package librarymanagement.controller;

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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private MockMvcTester mockMvcTester;

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
        BookTestData.BookData book = BookTestData.getNextBookData();

        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book.JSON)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo(book.JSON);
    }

    @Test
    void testAddInvalidBook() {
        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BookTestData.InvalidBookNoTitleInvalidIsbn.JSON)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(testResult).bodyJson().extractingPath("isbn")
                .isEqualTo("ISBN must be 10 digits (last can be X) or 13 digits starting with 978/979");
        assertThat(testResult).bodyJson().extractingPath("title")
                .isEqualTo("Title cannot be blank");
    }

    @Test
    void testAddDuplicateIsbnBook() {
        BookTestData.BookData book = BookTestData.getNextBookData();

        // Add once
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book.JSON)
                .exchange();

        // Add the same book again
        MvcTestResult testResult = mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book.JSON)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.CONFLICT);
        assertThat(testResult).bodyJson().extractingPath("error")
                .isEqualTo("A book with this ISBN already exists: " + book.ISBN);
    }

    @Test
    void testSearchBooksByIsbn() {
        BookTestData.BookData book = BookTestData.getNextBookData();

        // Add one book
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book.JSON)
                .exchange();

        // Search by ISBN
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/books/search")
                .param("isbn", book.ISBN)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("totalElements").isEqualTo(1);
    }

    @Test
    void testSearchBooksByYear() {
        BookTestData.BookData book1 = BookTestData.getNextBookData();
        BookTestData.BookData book2 = BookTestData.getNextBookData();

        // Create second book with same year as first
        String book2SameYearJson = """
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
                """.formatted(book2.TITLE, book1.PUBLICATION_YEAR, book2.ISBN, book2.AUTHOR1, book2.AUTHOR2);

        // Add both books
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book1.JSON)
                .exchange();
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book2SameYearJson)
                .exchange();

        // Search by year
        MvcTestResult result = mockMvcTester.get()
                .uri("/api/books/search")
                .param("publicationYear", book1.PUBLICATION_YEAR.toString())
                .exchange();

        assertThat(result).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("totalElements").isEqualTo(2);
    }

    @Test
    void testUpdateBook() {
        BookTestData.BookData book = BookTestData.getNextBookData();

        // Create a book to update
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book.JSON)
                .exchange();

        // Create updated version
        String updatedJson = """
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
                """.formatted(book.TITLE + "Updated", book.PUBLICATION_YEAR + 1, book.ISBN, book.AUTHOR1 + "Updated", book.AUTHOR2 + "Updated");

        // Update the book
        MvcTestResult updateResult = mockMvcTester.put()
                .uri("/api/books/" + book.ISBN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedJson)
                .exchange();

        assertThat(updateResult).hasStatus(HttpStatus.OK);
    }

    @Test
    void testUpdateBookChangeIsbn() {
        BookTestData.BookData book = BookTestData.getNextBookData();
        BookTestData.BookData differentBook = BookTestData.getNextBookData();

        // Create a book to update
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book.JSON)
                .exchange();

        // Try to change the ISBN using a different book's ISBN
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
                """.formatted(book.TITLE, differentBook.ISBN, book.AUTHOR1);

        MvcTestResult updateResult = mockMvcTester.put()
                .uri("/api/books/" + book.ISBN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidUpdateJson)
                .exchange();

        assertThat(updateResult).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(updateResult).bodyJson().extractingPath("error")
                .isEqualTo("Cannot change ISBN of an existing book");
    }

    @Test
    void testUpdateNonExistentBook() {
        BookTestData.BookData book = BookTestData.getNextBookData();

        MvcTestResult testResult = mockMvcTester.put()
                .uri("/api/books/" + book.ISBN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(book.JSON)
                .exchange();

        assertThat(testResult).hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().extractingPath("error")
                .isEqualTo("Book not found with ISBN: " + book.ISBN);
    }

    @Test
    void testDeleteBook() {
        BookTestData.BookData book = BookTestData.getNextBookData();

        // Create a book to delete
        mockMvcTester.post()
                .uri("/api/books")
                .contentType(MediaType.APPLICATION_JSON)
                .content(book.JSON)
                .exchange();

        // Delete the book
        assertThat(mockMvcTester.delete().uri("/api/books/" + book.ISBN))
                .hasStatus(HttpStatus.NO_CONTENT);

        // Verify the book is deleted
        MvcTestResult getResult = mockMvcTester.get().uri("/api/books/" + book.ISBN).exchange();
        assertThat(getResult).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(getResult).bodyJson().extractingPath("error")
                .isEqualTo("Book not found with ISBN: " + book.ISBN);
    }
}