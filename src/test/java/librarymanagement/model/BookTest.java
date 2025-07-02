package librarymanagement.model;

import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BookTest {

    @Test
    void testBookCreation() {
        String title = BookTestData.getNextBookData().TITLE;
        Set<Author> authors = new LinkedHashSet<>();
        authors.add(new Author(BookTestData.getCurrentBookData().AUTHOR1));
        authors.add(new Author(BookTestData.getCurrentBookData().AUTHOR2));
        Integer publicationYear = BookTestData.getCurrentBookData().PUBLICATION_YEAR;
        String isbn = BookTestData.getCurrentBookData().ISBN;

        Book book = new Book();
        book.setTitle(title);
        book.setAuthors(authors);
        book.setPublicationYear(publicationYear);
        book.setIsbn(isbn);

        assertThat(book.getTitle()).isEqualTo(title);
        assertThat(book.getAuthors()).isEqualTo(authors);
        assertThat(book.getPublicationYear()).isEqualTo(publicationYear);
        assertThat(book.getIsbn()).isEqualTo(isbn);
    }

    @Test
    void testBookEquality() {
        Book book1 = new Book();
        BookTestData.BookData bookData = BookTestData.getNextBookData();
        book1.setIsbn(bookData.ISBN);
        book1.setTitle(bookData.TITLE);
        book1.setPublicationYear(bookData.PUBLICATION_YEAR);
        book1.getAuthors().add(new Author(bookData.AUTHOR1));
        book1.getAuthors().add(new Author(bookData.AUTHOR2));

        Book book2 = new Book();
        book2.setIsbn(bookData.ISBN);
        book2.setTitle(bookData.TITLE);
        book2.setPublicationYear(bookData.PUBLICATION_YEAR);
        book2.getAuthors().add(new Author(bookData.AUTHOR1));
        book2.getAuthors().add(new Author(bookData.AUTHOR2));

        assertThat(book1.hashCode()).isEqualTo(book2.hashCode());
        assertThat(book1).isEqualTo(book2);
    }
}
