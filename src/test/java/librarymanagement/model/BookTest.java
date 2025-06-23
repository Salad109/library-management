package librarymanagement.model;

import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BookTest {

    @Test
    void testBookCreation() {
        String title = BookTestData.ValidBook1.TITLE;
        Set<Author> authors = new HashSet<>();
        authors.add(new Author(BookTestData.ValidBook1.AUTHOR_1));
        authors.add(new Author(BookTestData.ValidBook1.AUTHOR_2));
        Integer publicationYear = BookTestData.ValidBook1.PUBLICATION_YEAR;
        String isbn = BookTestData.ValidBook1.ISBN;

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
}
