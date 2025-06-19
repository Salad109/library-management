package librarymanagement.repository;

import librarymanagement.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    @Query("SELECT DISTINCT b FROM Book b " +
            "LEFT JOIN b.authors a " +
            "WHERE (:title IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :title, '%'))) " +
            "AND (:authorName IS NULL OR LOWER(a.name) LIKE LOWER(CONCAT('%', :authorName, '%'))) " +
            "AND (:year IS NULL OR b.publicationYear = :year)" +
            "AND (:isbn IS NULL OR b.isbn = :isbn)")
    List<Book> searchBooks(@Param("title") String title,
                           @Param("authorName") String authorName,
                           @Param("year") Integer year,
                           @Param("isbn") String isbn);

    boolean existsByIsbn(String isbn);
}
