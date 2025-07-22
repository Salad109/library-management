package librarymanagement.repository;

import librarymanagement.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {
    // If no parameters provided, return all books
    @Query(value = "SELECT DISTINCT b.* FROM books b " +
            "LEFT JOIN books_authors ba ON b.isbn = ba.books_isbn " +
            "LEFT JOIN authors a ON ba.authors_name = a.name " +
            "WHERE (:title IS NULL OR b.title ILIKE CONCAT('%', :title, '%')) " +
            "AND (:authorName IS NULL OR a.name ILIKE CONCAT('%', :authorName, '%')) " +
            "AND (:year IS NULL OR b.publication_year = :year) " +
            "AND (:isbn IS NULL OR b.isbn = :isbn)",
            nativeQuery = true)
    Page<Book> searchBooks(@Param("title") String title,
                           @Param("authorName") String authorName,
                           @Param("year") Integer year,
                           @Param("isbn") String isbn,
                           Pageable pageable);

    Optional<Book> findByIsbn(String isbn);
}
