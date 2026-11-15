package librarymanagement.copies;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CopyRepository extends JpaRepository<Copy, Long>, JpaSpecificationExecutor<Copy> {

    Optional<Copy> findFirstByBookIsbnAndStatusOrderById(String isbn, CopyStatus status);

    boolean existsByBookIsbn(String isbn);

    long countByCustomerIdAndStatus(Long customerId, CopyStatus status);
}
