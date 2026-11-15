package librarymanagement.books;

import librarymanagement.constants.Messages;
import librarymanagement.utils.DataBuilder;
import librarymanagement.utils.IntegrationTest;
import librarymanagement.utils.TestISBNGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

class BookControllerTest extends IntegrationTest {

    @Autowired
    private MockMvcTester mockMvcTester;

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testGetBookAfterCopiesAdded() {
        String isbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, isbn, "Test Book", "Test Author"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestCopy(mockMvcTester, isbn, 3))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult result = mockMvcTester.get().uri("/api/books/" + isbn).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("availableCopies").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("authors[0].name").isEqualTo("Test Author");
        assertThat(mockMvcTester.get().uri("/api/books"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("content[0].availableCopies")
                .isEqualTo(3);
    }

    @Test
    void testGetNonExistentBook() {
        String isbn = "987654321X";

        assertThat(mockMvcTester.get().uri("/api/books/" + isbn))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.BOOK_NOT_FOUND + isbn);
    }

    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void searchBook() {
        assertThat(DataBuilder.createTestBook(mockMvcTester, TestISBNGenerator.next(), "1984", "Jorjor Wel"))
                .hasStatus(HttpStatus.CREATED);

        assertThat(mockMvcTester.get().uri("/api/books/search?q=1984"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("page.totalElements")
                .isEqualTo(1);
    }

    /**
     * 1. Create a book with the term in its title, one with it in its author, and one with neither.
     * 2. The search finds the first two, ordered by title.
     */
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void searchMatchesTitleOrAuthor() {
        assertThat(DataBuilder.createTestBook(mockMvcTester, TestISBNGenerator.next(), "Zebra Kingdom", "Someone Else"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestBook(mockMvcTester, TestISBNGenerator.next(), "Carrie", "Stephen King"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestBook(mockMvcTester, TestISBNGenerator.next(), "Unrelated", "Nobody"))
                .hasStatus(HttpStatus.CREATED);

        MvcTestResult result = mockMvcTester.get().uri("/api/books/search?q=king").exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("page.totalElements").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("content[0].title").isEqualTo("Carrie");
        assertThat(result).bodyJson().extractingPath("content[1].title").isEqualTo("Zebra Kingdom");
    }

    /**
     * 1. Create two books.
     * 2. The list and the search are ordered by title unless a sort says otherwise.
     * 3. Books that tie on the sorted field are ordered by ISBN.
     * 4. A sort by a field that isn't allowed, no field at all, or ignoring case is rejected.
     */
    @Test
    @WithMockUser(roles = "LIBRARIAN")
    void testSortBooks() {
        // Alpha is created first with the higher ISBN, so insertion order and ISBN order differ
        String lowerIsbn = TestISBNGenerator.next();
        String higherIsbn = TestISBNGenerator.next();
        assertThat(DataBuilder.createTestBook(mockMvcTester, higherIsbn, "Alpha", "Some Author"))
                .hasStatus(HttpStatus.CREATED);
        assertThat(DataBuilder.createTestBook(mockMvcTester, lowerIsbn, "Beta", "Some Author"))
                .hasStatus(HttpStatus.CREATED);

        assertFirstTitle("/api/books", "Alpha");
        assertFirstTitle("/api/books?sort=title,desc", "Beta");
        assertFirstTitle("/api/books/search?q=a&sort=title,desc", "Beta");

        // Both books have the same year
        assertFirstTitle("/api/books?sort=publicationYear", "Beta");
        assertFirstTitle("/api/books/search?q=a&sort=publicationYear", "Beta");

        assertThat(mockMvcTester.get().uri("/api/books?sort=version"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.SORT_FIELD_UNSUPPORTED + "version");
        assertThat(mockMvcTester.get().uri("/api/books?sort=desc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.SORT_FIELD_MISSING);
        assertThat(mockMvcTester.get().uri("/api/books/search?q=a&sort=publicationYear,ignorecase"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("detail")
                .isEqualTo(Messages.SORT_IGNORE_CASE_UNSUPPORTED);
    }

    private void assertFirstTitle(String uri, String expected) {
        assertThat(mockMvcTester.get().uri(uri))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("content[0].title")
                .isEqualTo(expected);
    }
}
