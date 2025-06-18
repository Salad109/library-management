package librarymanagement.repository;

import librarymanagement.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // List<Book> findByTitle(String title);

    // List<Book> findByAuthor(String author);

    // List<Book> findByPublicationYearGreaterThan(int publicationYear);

    boolean existsByIsbn(String isbn);
}
