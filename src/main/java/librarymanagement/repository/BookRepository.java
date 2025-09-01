package librarymanagement.repository;

import librarymanagement.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {

    boolean existsByIsbn(String isbn);

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.authors WHERE b.isbn = :isbn")
    Optional<Book> findByIsbnWithAuthors(String isbn);

    @Query("SELECT b.isbn FROM Book b ORDER BY b.title")
    Page<String> findAllIsbns(Pageable pageable);

    @Query("SELECT DISTINCT b FROM Book b LEFT JOIN FETCH b.authors WHERE b.isbn IN :isbns ORDER BY b.title")
    List<Book> findByIsbnsWithAuthors(@Param("isbns") List<String> isbns);

    @Query("SELECT b.isbn FROM Book b WHERE b.title ILIKE CONCAT('%', :term, '%') ORDER BY b.title")
    Page<String> findIsbnsByTitleContaining(@Param("term") String term, Pageable pageable);

    @Query("SELECT DISTINCT b.isbn FROM Book b JOIN b.authors a " +
            "WHERE a.name ILIKE CONCAT('%', :term, '%') ORDER BY b.isbn")
    Page<String> findIsbnsByAuthorContaining(@Param("term") String term, Pageable pageable);
}