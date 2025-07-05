package librarymanagement.model;

import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorTest {

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

        BookTestData.BookData oldBookData = BookTestData.getNextBookData();
        Book oldBook = new Book(
                oldBookData.ISBN,
                oldBookData.TITLE,
                null,
                oldBookData.PUBLICATION_YEAR
        );

        BookTestData.BookData newBookData = BookTestData.getNextBookData();
        Book newBook = new Book(
                newBookData.ISBN,
                newBookData.TITLE,
                null,
                newBookData.PUBLICATION_YEAR
        );

        // Test setting null
        author.setBooks(null);
        assertThat(author.getBooks()).isNotNull().isEmpty();


        // Test replacing books
        author.setBooks(Set.of(oldBook));
        author.setBooks(Set.of(newBook));

        // Old book should no longer be associated with the author
        assertThat(oldBook.getAuthors()).doesNotContain(author);
        assertThat(newBook.getAuthors()).contains(author);
    }
}
