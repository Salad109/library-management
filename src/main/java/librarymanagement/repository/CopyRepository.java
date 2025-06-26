package librarymanagement.repository;

import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CopyRepository extends JpaRepository<Copy, Long> {
    List<Copy> findByBookIsbn(String isbn);

    List<Copy> findByBookIsbnAndStatus(String isbn, CopyStatus status);

    long countByBookIsbnAndStatus(String isbn, CopyStatus status);
}
