package librarymanagement.model;

import librarymanagement.testdata.BookTestData;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BookTest {

    @Test
    void testBookCreation() {
        String title = BookTestData.getNextBook().TITLE;
        Set<Author> authors = new HashSet<>();
        authors.add(new Author(BookTestData.getCurrentBook().AUTHOR1));
        authors.add(new Author(BookTestData.getCurrentBook().AUTHOR2));
        Integer publicationYear = BookTestData.getCurrentBook().PUBLICATION_YEAR;
        String isbn = BookTestData.getCurrentBook().ISBN;

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
