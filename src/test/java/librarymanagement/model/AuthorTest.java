package librarymanagement.model;

import librarymanagement.utils.BookTestData;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorTest {

    @Test
    void testAuthorCreation() {
        Author author1 = new Author("Joe Mama");
        Author author2 = new Author("Joe Mama");

        Book book = new Book(
                BookTestData.TestBook1.ISBN,
                BookTestData.TestBook1.TITLE,
                null,
                BookTestData.TestBook1.PUBLICATION_YEAR
        );

        author1.getBooks().add(book);
        author2.setBooks(Set.of(book));

        assertThat(author1).isEqualTo(author2);
    }

    @Test
    void testChangeBookOwnership() {
        Author author = new Author("Joe Mama");

        Book oldBook = new Book(
                BookTestData.TestBook1.ISBN,
                BookTestData.TestBook1.TITLE,
                null,
                BookTestData.TestBook1.PUBLICATION_YEAR
        );

        Book newBook = new Book(
                BookTestData.TestBook2.ISBN,
                BookTestData.TestBook2.TITLE,
                null,
                BookTestData.TestBook2.PUBLICATION_YEAR
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
