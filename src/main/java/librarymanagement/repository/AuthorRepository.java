package librarymanagement.repository;

import librarymanagement.model.Author;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, String> {
    @Query("SELECT a.name FROM Author a ORDER BY a.name")
    Page<String> findAllNames(Pageable pageable);

    @Query("SELECT DISTINCT a FROM Author a " +
            "LEFT JOIN FETCH a.books " +
            "WHERE a.name IN :names ORDER BY a.name")
    List<Author> findByNamesWithBooks(@Param("names") List<String> names);
}
