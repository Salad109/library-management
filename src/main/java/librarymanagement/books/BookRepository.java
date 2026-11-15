package librarymanagement.books;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, String> {

    boolean existsByIsbn(String isbn);

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.authors WHERE b.isbn = :isbn")
    Optional<Book> findByIsbnWithAuthors(String isbn);

    // A UNION, not an OR: Postgres can't use the trigram indexes for "title matches OR an author matches"
    @Query("""
            SELECT b FROM Book b WHERE b.isbn IN (
                SELECT byTitle.isbn FROM Book byTitle WHERE byTitle.title ILIKE CONCAT('%', :term, '%')
                UNION
                SELECT byAuthor.isbn FROM Book byAuthor JOIN byAuthor.authors a WHERE a.name ILIKE CONCAT('%', :term, '%'))
            """)
    Page<Book> findByTitleOrAuthorContaining(String term, Pageable pageable);
}