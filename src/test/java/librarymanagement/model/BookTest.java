package librarymanagement.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BookTest {

    @Test
    void testBookConstructor() {
        String title = "Test Book";
        String author = "Joe Mama";
        Integer publicationYear = 2025;
        String isbn = "1234567890";

        Book book = new Book(title, author, publicationYear, isbn);

        assertThat(book.getTitle()).isEqualTo(title);
        assertThat(book.getAuthor()).isEqualTo(author);
        assertThat(book.getPublicationYear()).isEqualTo(publicationYear);
        assertThat(book.getIsbn()).isEqualTo(isbn);
    }

    @Test
    void testBookSetters() {
        Book book = new Book();
        String title = "Test Book 2";
        String author = "Jane Mama";
        Integer publicationYear = 2077;
        String isbn = "123456789X";

        book.setTitle(title);
        book.setAuthor(author);
        book.setPublicationYear(publicationYear);
        book.setIsbn(isbn);

        assertThat(book.getTitle()).isEqualTo(title);
        assertThat(book.getAuthor()).isEqualTo(author);
        assertThat(book.getPublicationYear()).isEqualTo(publicationYear);
        assertThat(book.getIsbn()).isEqualTo(isbn);
    }
}
