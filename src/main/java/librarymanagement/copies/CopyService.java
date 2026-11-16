package librarymanagement.copies;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import librarymanagement.SortFields;
import librarymanagement.books.Book;
import librarymanagement.books.BookService;
import librarymanagement.constants.Messages;
import librarymanagement.customers.Customer;
import librarymanagement.customers.CustomerService;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class CopyService {
    private static final Set<String> SORT_FIELDS = Set.of("id", "status");
    private static final Logger log = LoggerFactory.getLogger(CopyService.class);
    private final CopyRepository copyRepository;
    private final BookService bookService;
    private final CustomerService customerService;
    private final int maxActiveReservations;
    private final Duration reservationHold;
    private final int maxReservationExtensions;
    private final Duration loanPeriod;

    public CopyService(CopyRepository copyRepository, BookService bookService, CustomerService customerService,
                       @Value("${library.reservations.max-active}") int maxActiveReservations,
                       @Value("${library.reservations.hold-days}") int reservationHoldDays,
                       @Value("${library.reservations.max-extensions}") int maxReservationExtensions,
                       @Value("${library.loans.period-days}") int loanPeriodDays) {
        this.copyRepository = copyRepository;
        this.bookService = bookService;
        this.customerService = customerService;
        this.maxActiveReservations = maxActiveReservations;
        this.reservationHold = Duration.ofDays(reservationHoldDays);
        this.maxReservationExtensions = maxReservationExtensions;
        this.loanPeriod = Duration.ofDays(loanPeriodDays);
    }

    // Hibernate's ILIKE for the trigram indexes
    private static Predicate containsIgnoreCase(CriteriaBuilder cb, Expression<String> field, String term) {
        HibernateCriteriaBuilder hibernateBuilder = (HibernateCriteriaBuilder) cb;
        return hibernateBuilder.ilike(field, "%" + term + "%");
    }

    // Null filters are ignored
    @Transactional(readOnly = true)
    public Page<CopyResponse> getCopies(String isbn, String title, Long customerId, String customerLastName,
                                        boolean overdue, Pageable pageable) {
        SortFields.requireAllowed(pageable, SORT_FIELDS);
        Pageable stablePageable = SortFields.withTiebreaker(pageable, "id");

        List<Specification<Copy>> filters = new ArrayList<>();
        if (isbn != null) {
            filters.add((copy, _, cb) ->
                    cb.equal(copy.get("book").get("isbn"), isbn));
        }
        if (title != null) {
            filters.add((copy, _, cb) ->
                    containsIgnoreCase(cb, copy.join("book").get("title"), title));
        }
        if (customerId != null) {
            filters.add((copy, _, cb) ->
                    cb.equal(copy.get("customer").get("id"), customerId));
        }
        if (customerLastName != null) {
            filters.add((copy, _, cb) ->
                    containsIgnoreCase(cb, copy.join("customer").get("lastName"), customerLastName));
        }
        if (overdue) {
            Instant now = Instant.now();
            filters.add((copy, _, cb) -> cb.and(
                    cb.equal(copy.get("status"), CopyStatus.BORROWED),
                    cb.lessThan(copy.get("dueAt"), now)));
        }

        return copyRepository.findAll(Specification.allOf(filters), stablePageable).map(CopyResponse::from);
    }

    @Transactional(readOnly = true)
    public CopyResponse getCopyById(Long id) {
        return CopyResponse.from(getCopyOrThrow(id));
    }

    @Transactional
    public List<CopyResponse> addCopies(String isbn, int quantity) {
        Book book = bookService.getBookOrThrow(isbn);

        List<Copy> savedCopies = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            Copy copy = new Copy();
            copy.setBook(book);
            copy.setStatus(CopyStatus.AVAILABLE);
            savedCopies.add(save(copy));
        }

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
        existingCopy.setDueAt(null);
        existingCopy.setRenewals(0);
        Copy savedCopy = save(existingCopy);

        log.info("Copy with ID: {} marked as lost", savedCopy.getId());
        return CopyResponse.from(savedCopy);
    }

    // Retryable: concurrent reservations pick the same first available copy, so the loser's retry picks the next
    @Transactional
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 2, delay = 50)
    public CopyResponse reserveAnyAvailableCopy(String isbn, Long customerId) {
        if (copyRepository.countByCustomerIdAndStatus(customerId, CopyStatus.RESERVED) >= maxActiveReservations) {
            log.info("Customer ID: {} has reached the reservation limit", customerId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_RESERVATION_LIMIT + maxActiveReservations);
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
        copyToReserve.setDueAt(Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(reservationHold));
        copyToReserve.setRenewals(0);
        Copy savedCopy = save(copyToReserve);

        log.info("Copy with ID: {} reserved for customer ID: {} until {}", copyId, customerId, savedCopy.getDueAt());
        return CopyResponse.from(savedCopy);
    }

    @Transactional
    public void cancelReservation(Long copyId, Long customerId) {
        release(copyId, customerId, CopyStatus.RESERVED, Messages.COPY_NOT_RESERVED);
        log.info("Cancelled reservation for copy with ID: {}", copyId);
    }

    @Transactional
    public CopyResponse extendReservation(Long copyId, Long customerId) {
        Copy copy = getCopyOrThrow(copyId);
        requireStatusAndOwner(copy, CopyStatus.RESERVED, Messages.COPY_NOT_RESERVED, customerId);
        if (copy.getRenewals() >= maxReservationExtensions) {
            log.warn("Reservation for copy with ID: {} was already extended {} times", copyId, copy.getRenewals());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_EXTENSION_LIMIT + maxReservationExtensions);
        }

        copy.setDueAt(copy.getDueAt().plus(reservationHold));
        copy.setRenewals(copy.getRenewals() + 1);
        Copy savedCopy = save(copy);
        log.info("Reservation for copy with ID: {} extended until {}", copyId, savedCopy.getDueAt());
        return CopyResponse.from(savedCopy);
    }

    @Scheduled(cron = "${library.reservations.expiry-cron}")
    @Transactional
    public void expireReservations() {
        List<Copy> expiredCopies = copyRepository.findByStatusAndDueAtBefore(CopyStatus.RESERVED, Instant.now());
        for (Copy copy : expiredCopies) {
            log.info("Reservation for copy with ID: {} by customer ID: {} expired", copy.getId(), copy.getCustomer().getId());
            copy.setCustomer(null);
            copy.setStatus(CopyStatus.AVAILABLE);
            copy.setDueAt(null);
            copy.setRenewals(0);
            save(copy);
        }
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
        copy.setDueAt(Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(loanPeriod));
        copy.setRenewals(0);
        Copy savedCopy = save(copy);
        log.info("Copy with ID: {} checked out to customer ID: {}, due {}", copyId, customerId, savedCopy.getDueAt());
        return CopyResponse.from(savedCopy);
    }

    @Transactional
    public CopyResponse renewLoan(Long copyId, Long customerId) {
        Copy copy = getCopyOrThrow(copyId);
        requireStatusAndOwner(copy, CopyStatus.BORROWED, Messages.COPY_NOT_BORROWED, customerId);

        copy.setDueAt(Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(loanPeriod));
        copy.setRenewals(copy.getRenewals() + 1);
        Copy savedCopy = save(copy);
        log.info("Loan of copy with ID: {} renewed until {}", copyId, savedCopy.getDueAt());
        return CopyResponse.from(savedCopy);
    }

    // Helpers

    // Makes a borrowed or reserved copy available again; only its own customer can release it
    private Copy release(Long copyId, Long customerId, CopyStatus expectedStatus, String wrongStatusMessage) {
        Copy copy = getCopyOrThrow(copyId);
        requireStatusAndOwner(copy, expectedStatus, wrongStatusMessage, customerId);
        copy.setCustomer(null);
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setDueAt(null);
        copy.setRenewals(0);
        return save(copy);
    }

    private void requireStatusAndOwner(Copy copy, CopyStatus expectedStatus, String wrongStatusMessage, Long customerId) {
        if (copy.getStatus() != expectedStatus) {
            log.warn("Copy with ID: {} is not {}, current status: {}", copy.getId(), expectedStatus, copy.getStatus());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, wrongStatusMessage + copy.getStatus());
        }
        Long ownerId = copy.getCustomer().getId();
        if (!ownerId.equals(customerId)) {
            log.warn("Copy with ID: {} belongs to customer ID: {}, not {}", copy.getId(), ownerId, customerId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, Messages.COPY_WRONG_CUSTOMER + ownerId);
        }
    }

    public Copy getCopyOrThrow(Long id) {
        return copyRepository.findById(id).orElseThrow(() -> {
            log.warn("Copy not found with ID: {}", id);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.COPY_NOT_FOUND + id);
        });
    }

    // Every copy write must go through here. It updates the book's availableCopies cache
    private Copy save(Copy copy) {
        Copy savedCopy = copyRepository.save(copy);
        bookService.evictCachedBook(savedCopy.getBook().getIsbn());
        return savedCopy;
    }
}
