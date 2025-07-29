package librarymanagement.repository;

import librarymanagement.model.Author;
import librarymanagement.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

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
        Page<Book> books = bookRepository.findAll(PageRequest.of(0, 10));
        assertThat(books).isNotNull();
    }

    @Test
    void shouldSearchBooksByTitle() {
        // Put a book in the database
        Author author = new Author("The Goober");
        Book book = new Book("112233445X", "The Goober Lore", Set.of(author), 2025);

        testEntityManager.persistAndFlush(book);

        Page<Book> results = bookRepository.findByAuthorContaining("Goober", PageRequest.of(0, 10));

        assertThat(results).hasSize(1);
        assertThat(results.getContent().getFirst().getTitle()).isEqualTo("The Goober Lore");
    }
}