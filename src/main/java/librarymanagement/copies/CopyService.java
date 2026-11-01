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

    @Transactional(readOnly = true)
    public Page<CopyResponse> getAllCopies(Pageable pageable) {
        return loadPage(copyRepository.findAllIds(pageable));
    }

    @Transactional(readOnly = true)
    public CopyResponse getCopyById(Long id) {
        return CopyResponse.from(getCopyOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<CopyResponse> getCopiesByBookIsbn(String isbn, Pageable pageable) {
        return loadPage(copyRepository.findIdsByBookIsbn(isbn, pageable));
    }

    @Transactional(readOnly = true)
    public Page<CopyResponse> getCopiesByCustomerId(Long customerId, Pageable pageable) {
        return loadPage(copyRepository.findIdsByCustomerId(customerId, pageable));
    }

    @Transactional(readOnly = true)
    public Page<CopyResponse> getCopiesByBookTitle(String title, Pageable pageable) {
        return loadPage(copyRepository.findIdsByBookTitle(title, pageable));
    }

    @Transactional(readOnly = true)
    public Page<CopyResponse> getCopiesByCustomerLastName(String lastName, Pageable pageable) {
        return loadPage(copyRepository.findIdsByCustomerLastName(lastName, pageable));
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
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.BORROWED) {
            log.warn("Copy with ID: {} is not borrowed, current status: {}", copyId, existingCopy.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_NOT_BORROWED + existingCopy.getStatus());
        }

        Customer customer = customerService.getCustomerOrThrow(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            log.warn("Copy with ID: {} is reserved for another customer, current customer ID: {}", copyId, existingCopy.getCustomer().getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setCustomer(null);
        existingCopy.setStatus(CopyStatus.AVAILABLE);

        Copy savedCopy = save(existingCopy);
        log.info("Copy with ID: {} returned successfully", savedCopy.getId());
        return CopyResponse.from(savedCopy);
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
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.RESERVED) {
            log.warn("Copy with ID: {} is not reserved, current status: {}", copyId, existingCopy.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_NOT_RESERVED + existingCopy.getStatus());
        }

        Customer customer = customerService.getCustomerOrThrow(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            log.warn("Copy with ID: {} is reserved for customer ID: {}, not for customer ID: {}", copyId, existingCopy.getCustomer().getId(), customerId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setCustomer(null);
        existingCopy.setStatus(CopyStatus.AVAILABLE);

        Copy savedCopy = save(existingCopy);
        log.info("Cancelled reservation for copy with ID: {}", savedCopy.getId());
    }

    @Transactional
    public CopyResponse checkout(Long copyId, Long customerId) {
        Copy copy = getCopyOrThrow(copyId);
        Customer customer = customerService.getCustomerOrThrow(customerId);

        // Reserved copy checkout
        if (copy.getStatus() == CopyStatus.RESERVED) {
            return CopyResponse.from(checkoutReservedCopy(copy, customer));
        }

        // Direct checkout
        if (copy.getStatus() == CopyStatus.AVAILABLE) {
            return CopyResponse.from(checkoutAvailableCopy(copy, customer));
        }

        log.info("Copy with ID: {} is not available for checkout, current status: {}", copyId, copy.getStatus());
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_UNAVAILABLE_FOR_CHECKOUT + copy.getStatus());
    }

    private Copy checkoutReservedCopy(Copy copy, Customer customer) {
        if (!copy.getCustomer().getId().equals(customer.getId())) {
            log.info("Copy with ID: {} is reserved for another customer ID: {}, cannot checkout", copy.getId(), copy.getCustomer().getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_RESERVED_FOR_ANOTHER_CUSTOMER + copy.getCustomer().getId());
        }
        copy.setStatus(CopyStatus.BORROWED);
        Copy savedCopy = save(copy);
        log.info("Reserved copy with ID: {} checked out successfully for customer ID: {}", savedCopy.getId(), customer.getId());
        return savedCopy;
    }

    private Copy checkoutAvailableCopy(Copy copy, Customer customer) {
        copy.setCustomer(customer);
        copy.setStatus(CopyStatus.BORROWED);

        Copy savedCopy = save(copy);

        log.info("Available copy with ID: {} checked out successfully for customer ID: {}", savedCopy.getId(), customer.getId());
        return savedCopy;
    }

    // Helpers

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
