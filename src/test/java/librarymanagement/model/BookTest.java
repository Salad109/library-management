package librarymanagement.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class BookTest {

    @Test
    void testBookCreation() {
        String title = "Test Book";
        Set<Author> authors = new HashSet<>();
        authors.add(new Author("Joe Mama"));
        Integer publicationYear = 2025;
        String isbn = "1234567890";

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
