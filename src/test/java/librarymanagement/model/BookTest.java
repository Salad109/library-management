package librarymanagement.model;

import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BookTest {

    @Test
    void testBookCreation() {
        String isbn = BookTestData.TestBook1.ISBN;
        String title = BookTestData.TestBook1.TITLE;
        Set<Author> authors = new LinkedHashSet<>();
        authors.add(new Author(BookTestData.TestBook1.AUTHOR_NAME));
        Integer publicationYear = BookTestData.TestBook1.PUBLICATION_YEAR;

        Book book = new Book();
        book.setIsbn(isbn);
        book.setTitle(title);
        book.setAuthors(authors);
        book.setPublicationYear(publicationYear);

        assertThat(book.getIsbn()).isEqualTo(isbn);
        assertThat(book.getTitle()).isEqualTo(title);
        assertThat(book.getAuthors()).isEqualTo(authors);
        assertThat(book.getPublicationYear()).isEqualTo(publicationYear);
    }

    @Test
    void testBookEquality() {
        Book book1 = new Book();
        book1.setIsbn(BookTestData.TestBook1.ISBN);
        book1.setTitle(BookTestData.TestBook1.TITLE);
        book1.setPublicationYear(BookTestData.TestBook1.PUBLICATION_YEAR);
        book1.getAuthors().add(new Author(BookTestData.TestBook1.AUTHOR_NAME));

        Book book2 = new Book();
        book2.setIsbn(BookTestData.TestBook1.ISBN);
        book2.setTitle(BookTestData.TestBook1.TITLE);
        book2.setPublicationYear(BookTestData.TestBook1.PUBLICATION_YEAR);
        book2.getAuthors().add(new Author(BookTestData.TestBook1.AUTHOR_NAME));

        assertThat(book1.hashCode()).isEqualTo(book2.hashCode());
        assertThat(book1).isEqualTo(book2);
    }
}
