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

    Optional<Book> findByIsbn(String isbn);

    @Query("SELECT b FROM Book b WHERE b.title ILIKE CONCAT('%', :term, '%')")
    Page<Book> findByTitleContaining(@Param("term") String term, Pageable pageable);

    @Query("SELECT DISTINCT b FROM Book b JOIN b.authors a WHERE a.name ILIKE CONCAT('%', :term, '%')")
    Page<Book> findByAuthorContaining(@Param("term") String term, Pageable pageable);
}