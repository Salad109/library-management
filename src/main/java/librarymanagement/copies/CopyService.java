package librarymanagement.copies;

import librarymanagement.books.Book;
import librarymanagement.books.BookService;
import librarymanagement.constants.Messages;
import librarymanagement.customers.Customer;
import librarymanagement.customers.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CopyService {
    public static final int MAX_ACTIVE_RESERVATIONS = 3;
    private static final Logger log = LoggerFactory.getLogger(CopyService.class);
    private final CopyRepository copyRepository;
    private final BookService bookService;
    private final CustomerService customerService;

    public CopyService(CopyRepository copyRepository, BookService bookService, CustomerService customerService) {
        this.copyRepository = copyRepository;
        this.bookService = bookService;
        this.customerService = customerService;
    }

    // Null filters are ignored
    @Transactional(readOnly = true)
    public Page<CopyResponse> getCopies(String isbn, String title, Long customerId, String customerLastName,
                                        Pageable pageable) {
        return loadPage(copyRepository.findIds(isbn, title, customerId, customerLastName, pageable));
    }

    @Transactional(readOnly = true)
    public CopyResponse getCopyById(Long id) {
        return CopyResponse.from(getCopyOrThrow(id));
    }

    @Transactional
    public List<CopyResponse> addCopies(String isbn, int quantity) {
        Book book = bookService.getBookOrThrow(isbn);

        List<Copy> copies = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            Copy copy = new Copy();
            copy.setBook(book);
            copy.setStatus(CopyStatus.AVAILABLE);
            copies.add(copy);
        }

        List<Copy> savedCopies = copyRepository.saveAll(copies);
        bookService.evictCachedBook(isbn);
        log.info("Added {} copies for book with ISBN: {}", savedCopies.size(), isbn);
        return savedCopies.stream()
                .map(CopyResponse::from)
                .toList();
    }

    @Transactional
    public CopyResponse returnCopy(Long copyId, Long customerId) {
        Copy copy = release(copyId, customerId, CopyStatus.BORROWED, Messages.COPY_NOT_BORROWED);
        log.info("Copy with ID: {} returned", copyId);
        return CopyResponse.from(copy);
    }

    @Transactional
    public CopyResponse markLost(Long copyId) {
        Copy existingCopy = getCopyOrThrow(copyId);

        existingCopy.setStatus(CopyStatus.LOST);
        Copy savedCopy = save(existingCopy);

        log.info("Copy with ID: {} marked as lost", savedCopy.getId());
        return CopyResponse.from(savedCopy);
    }

    // Retryable: concurrent reservations pick the same first available copy, so the loser's retry picks the next
    @Transactional
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 2, delay = 50)
    public CopyResponse reserveAnyAvailableCopy(String isbn, Long customerId) {
        if (copyRepository.countByCustomerIdAndStatus(customerId, CopyStatus.RESERVED) >= MAX_ACTIVE_RESERVATIONS) {
            log.info("Customer ID: {} has reached the reservation limit", customerId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_RESERVATION_LIMIT + MAX_ACTIVE_RESERVATIONS);
        }

        Optional<Copy> availableCopy = copyRepository.findFirstByBookIsbnAndStatusOrderById(isbn, CopyStatus.AVAILABLE);
        if (availableCopy.isEmpty()) {
            log.info("No available copies found for book with ISBN: {}", isbn);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.COPY_NO_AVAILABLE + isbn);
        }

        Copy copyToReserve = availableCopy.get();
        long copyId = copyToReserve.getId();

        Customer customer = customerService.getCustomerOrThrow(customerId);

        copyToReserve.setCustomer(customer);
        copyToReserve.setStatus(CopyStatus.RESERVED);
        Copy savedCopy = save(copyToReserve);

        log.info("Copy with ID: {} reserved for customer ID: {}", copyId, customerId);
        return CopyResponse.from(savedCopy);
    }

    @Transactional
    public void cancelReservation(Long copyId, Long customerId) {
        release(copyId, customerId, CopyStatus.RESERVED, Messages.COPY_NOT_RESERVED);
        log.info("Cancelled reservation for copy with ID: {}", copyId);
    }

    // Available copies go to anyone, reserved ones only to the customer who reserved them
    @Transactional
    public CopyResponse checkout(Long copyId, Long customerId) {
        Copy copy = getCopyOrThrow(copyId);
        Customer customer = customerService.getCustomerOrThrow(customerId);
        if (copy.getStatus() != CopyStatus.AVAILABLE && copy.getStatus() != CopyStatus.RESERVED) {
            log.warn("Copy with ID: {} is not available for checkout, current status: {}", copyId, copy.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_UNAVAILABLE_FOR_CHECKOUT + copy.getStatus());
        }
        if (copy.getStatus() == CopyStatus.RESERVED && !copy.getCustomer().getId().equals(customerId)) {
            log.warn("Copy with ID: {} is reserved for customer ID: {}, not {}", copyId, copy.getCustomer().getId(), customerId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_RESERVED_FOR_ANOTHER_CUSTOMER + copy.getCustomer().getId());
        }

        copy.setCustomer(customer);
        copy.setStatus(CopyStatus.BORROWED);
        Copy savedCopy = save(copy);
        log.info("Copy with ID: {} checked out to customer ID: {}", copyId, customerId);
        return CopyResponse.from(savedCopy);
    }

    // Helpers

    // Makes a borrowed or reserved copy available again; only its own customer can release it
    private Copy release(Long copyId, Long customerId, CopyStatus expectedStatus, String wrongStatusMessage) {
        Copy copy = getCopyOrThrow(copyId);
        if (copy.getStatus() != expectedStatus) {
            log.warn("Copy with ID: {} is not {}, current status: {}", copyId, expectedStatus, copy.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, wrongStatusMessage + copy.getStatus());
        }
        Long ownerId = copy.getCustomer().getId();
        if (!ownerId.equals(customerId)) {
            log.warn("Copy with ID: {} belongs to customer ID: {}, not {}", copyId, ownerId, customerId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_WRONG_CUSTOMER + ownerId);
        }

        copy.setCustomer(null);
        copy.setStatus(CopyStatus.AVAILABLE);
        return save(copy);
    }

    public Copy getCopyOrThrow(Long id) {
        return copyRepository.findById(id).orElseThrow(() -> {
            log.warn("Copy not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.COPY_NOT_FOUND + id);
        });
    }

    private Copy save(Copy copy) {
        Copy savedCopy = copyRepository.save(copy);
        bookService.evictCachedBook(savedCopy.getBook().getIsbn());
        return savedCopy;
    }

    private Page<CopyResponse> loadPage(Page<Long> idPage) {
        if (idPage.isEmpty()) {
            return Page.empty(idPage.getPageable());
        }
        List<CopyResponse> copies = copyRepository.findByIdsWithAllRelations(idPage.getContent()).stream()
                .map(CopyResponse::from)
                .toList();
        return new PageImpl<>(copies, idPage.getPageable(), idPage.getTotalElements());
    }
}
