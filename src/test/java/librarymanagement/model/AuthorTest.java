package librarymanagement.model;

import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthorTest {
    @Test
    void testAuthorCreation() {
        Author author1 = new Author("Joe Mama");
        Author author2 = new Author("Joe Mama");

        BookTestData.BookData bookData = BookTestData.getNextBookData();
        Book book = new Book(
                bookData.ISBN,
                bookData.TITLE,
                null,
                bookData.PUBLICATION_YEAR
        );

        author1.getBooks().add(book);
        author2.setBooks(Set.of(book));

        assertThat(author1).isEqualTo(author2);
    }

    @Test
    void testChangeBookOwnership() {
        Author author = new Author("Joe Mama");

        BookTestData.BookData bookData1 = BookTestData.getNextBookData();
        Book book1 = new Book(
                bookData1.ISBN,
                bookData1.TITLE,
                null,
                bookData1.PUBLICATION_YEAR
        );
        BookTestData.BookData bookData2 = BookTestData.getNextBookData();
        Book book2 = new Book(
                bookData2.ISBN,
                bookData2.TITLE,
                null,
                bookData2.PUBLICATION_YEAR
        );
        author.setBooks(Set.of(book1));
        author.setBooks(Set.of(book2));

        assertThat(book1.getAuthors()).doesNotContain(author);
    }
}
