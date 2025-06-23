package librarymanagement.repository;

import librarymanagement.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, String> {
    Author findByName(String name);

    List<Author> findByNameContainingIgnoreCase(String name);
}
