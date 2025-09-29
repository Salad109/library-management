package librarymanagement.repository;

import librarymanagement.model.Author;
import librarymanagement.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BookRepositoryTest {
    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void testFindAllBooks() {
        Page<String> isbns = bookRepository.findAllIsbns(PageRequest.of(0, 10));
        List<Book> books = bookRepository.findByIsbnsWithAuthors(isbns.getContent());
        assertThat(books).isEmpty();

        Author author = new Author("The Goober");
        testEntityManager.persistAndFlush(author);

        Book book = new Book("1112223334", "This is a title", Set.of(author), 2025);
        testEntityManager.persistAndFlush(book);

        isbns = bookRepository.findAllIsbns(PageRequest.of(0, 10));
        books = bookRepository.findByIsbnsWithAuthors(isbns.getContent());
        assertThat(books).hasSize(1).extracting(Book::getTitle).containsExactly("This is a title");
    }

    @Test
    void testSearchBooksByTitle() {
        // Create author and book
        Author author = new Author("The Goober");
        testEntityManager.persistAndFlush(author);

        Book book = new Book("112233445X", "The Goober Lore", Set.of(author), 2025);

        // Put book in the database
        testEntityManager.persistAndFlush(book);

        // Search book by title
        Page<String> isbns = bookRepository.findIsbnsByTitleContaining("Goober", Pageable.ofSize(5));
        List<Book> foundBooks = bookRepository.findByIsbnsWithAuthors(isbns.getContent());

        assertThat(foundBooks).hasSize(1);
        assertThat(foundBooks.getFirst().getTitle()).isEqualTo("The Goober Lore");
    }
}