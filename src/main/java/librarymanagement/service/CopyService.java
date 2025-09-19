package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.model.Customer;
import librarymanagement.repository.CopyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CopyService {

    private static final Logger log = LoggerFactory.getLogger(CopyService.class);
    private final CopyRepository copyRepository;
    private final BookService bookService;
    private final CustomerService customerService;

    public CopyService(CopyRepository copyRepository, BookService bookService, CustomerService customerService) {
        this.copyRepository = copyRepository;
        this.bookService = bookService;
        this.customerService = customerService;
    }

    public Page<Copy> getAllCopies(Pageable pageable) {
        log.debug("Fetching all copies, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<Long> idPage = copyRepository.findAllIds(pageable);

        if (idPage.getContent().isEmpty()) {
            log.debug("No copies found, returning empty page");
            return Page.empty(pageable);
        }

        List<Copy> copies = copyRepository.findByIdsWithAllRelations(idPage.getContent());

        log.debug("Retrieved {} copies out of {} total", copies.size(), idPage.getTotalElements());

        return new PageImpl<>(copies, pageable, idPage.getTotalElements());
    }

    public Copy getCopyById(Long id) {
        return getCopyOrThrow(id);
    }

    public Page<Copy> getCopiesByBookIsbn(String isbn, Pageable pageable) {
        log.debug("Fetching copies for book ISBN: {}, page: {}, size: {}", isbn, pageable.getPageNumber(), pageable.getPageSize());

        Page<Long> idPage = copyRepository.findIdsByBookIsbn(isbn, pageable);

        if (idPage.getContent().isEmpty()) {
            log.debug("No copies found for ISBN: {}", isbn);
            return Page.empty(pageable);
        }

        List<Copy> copies = copyRepository.findByIdsWithAllRelations(idPage.getContent());

        log.debug("Retrieved {} copies for ISBN: {} out of {} total", copies.size(), isbn, idPage.getTotalElements());

        return new PageImpl<>(copies, pageable, idPage.getTotalElements());
    }

    public Page<Copy> getCopiesByCustomerId(Long customerId, Pageable pageable) {
        log.debug("Fetching copies for customer ID: {}, page: {}, size: {}", customerId, pageable.getPageNumber(), pageable.getPageSize());

        Page<Long> idPage = copyRepository.findIdsByCustomerId(customerId, pageable);

        if (idPage.getContent().isEmpty()) {
            log.debug("No copies found for customer ID: {}", customerId);
            return Page.empty(pageable);
        }

        List<Copy> copies = copyRepository.findByIdsWithAllRelations(idPage.getContent());

        log.debug("Retrieved {} copies for customer ID: {} out of {} total", copies.size(), customerId, idPage.getTotalElements());

        return new PageImpl<>(copies, pageable, idPage.getTotalElements());
    }

    @Transactional
    public List<Copy> addCopies(String isbn, int quantity) {
        log.debug("Adding {} copies for book with ISBN: {}", quantity, isbn);
        Book book = bookService.getBookByIsbn(isbn);

        List<Copy> copies = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            Copy copy = new Copy();
            copy.setBook(book);
            copy.setStatus(CopyStatus.AVAILABLE);
            copies.add(copy);
        }

        bookService.updateAvailableCopies(isbn, quantity);

        List<Copy> savedCopies = copyRepository.saveAll(copies);
        log.info("Added {} copies for book with ISBN: {}", savedCopies.size(), isbn);
        return savedCopies;
    }

    @Transactional
    public Copy returnCopy(Long copyId, Long customerId) {
        log.debug("Returning copy with ID: {} for customer ID: {}", copyId, customerId);
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.BORROWED) {
            log.warn("Copy with ID: {} is not borrowed, current status: {}", copyId, existingCopy.getStatus());
            throw new IllegalStateException(Messages.COPY_NOT_BORROWED + existingCopy.getStatus());
        }

        Customer customer = customerService.getCustomerById(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            log.warn("Copy with ID: {} is reserved for another customer, current customer ID: {}", copyId, existingCopy.getCustomer().getId());
            throw new IllegalStateException(Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setCustomer(null);
        existingCopy.setStatus(CopyStatus.AVAILABLE);

        Book book = existingCopy.getBook();
        bookService.updateAvailableCopies(book.getIsbn(), 1);

        Copy savedCopy = copyRepository.save(existingCopy);
        log.info("Copy with ID: {} returned successfully", savedCopy.getId());
        return savedCopy;
    }

    @Transactional
    public Copy markLost(Long copyId) {
        log.debug("Marking copy with ID: {} as lost", copyId);
        Copy existingCopy = getCopyOrThrow(copyId);

        if (existingCopy.getStatus() == CopyStatus.AVAILABLE) {
            Book book = existingCopy.getBook();
            bookService.updateAvailableCopies(book.getIsbn(), -1);
        }

        existingCopy.setStatus(CopyStatus.LOST);
        Copy savedCopy = copyRepository.save(existingCopy);
        log.info("Copy with ID: {} marked as lost", savedCopy.getId());
        return savedCopy;
    }

    @Transactional
    public Copy reserveAnyAvailableCopy(String isbn, Long customerId) {
        log.debug("Reserving any available copy for book with ISBN: {} for customer ID: {}", isbn, customerId);
        Optional<Copy> availableCopy = copyRepository.findFirstByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE);
        if (availableCopy.isEmpty()) {
            log.info("No available copies found for book with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.COPY_NO_AVAILABLE + isbn);
        }

        Copy copyToReserve = availableCopy.get();
        log.debug("Reserving copy with ID: {} for customer ID: {}", copyToReserve.getId(), customerId);
        return reserveCopy(copyToReserve.getId(), customerId);
    }

    private Copy reserveCopy(Long copyId, Long customerId) {
        Copy existingCopy = getCopyOrThrow(copyId);
        Customer customer = customerService.getCustomerById(customerId);

        Book book = existingCopy.getBook();
        bookService.updateAvailableCopies(book.getIsbn(), -1);

        existingCopy.setCustomer(customer);
        existingCopy.setStatus(CopyStatus.RESERVED);
        Copy savedCopy = copyRepository.save(existingCopy);
        log.info("Copy with ID: {} reserved for customer ID: {}", copyId, customerId);
        return savedCopy;
    }

    @Transactional
    public Copy cancelReservation(Long copyId, Long customerId) {
        log.debug("Cancelling reservation for copy with ID: {} for customer ID: {}", copyId, customerId);
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.RESERVED) {
            log.warn("Copy with ID: {} is not reserved, current status: {}", copyId, existingCopy.getStatus());
            throw new IllegalStateException(Messages.COPY_NOT_RESERVED + existingCopy.getStatus());
        }

        Customer customer = customerService.getCustomerById(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            log.warn("Copy with ID: {} is reserved for customer ID: {}, not for customer ID: {}", copyId, existingCopy.getCustomer().getId(), customerId);
            throw new IllegalStateException(Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setCustomer(null);
        existingCopy.setStatus(CopyStatus.AVAILABLE);

        Book book = existingCopy.getBook();
        bookService.updateAvailableCopies(book.getIsbn(), 1);

        Copy savedCopy = copyRepository.save(existingCopy);
        log.info("Cancelled reservation for copy with ID: {}", savedCopy.getId());
        return savedCopy;
    }

    @Transactional
    public Copy checkout(Long copyId, Long customerId) {
        log.debug("Checking out copy with ID: {} for customer ID: {}", copyId, customerId);
        Copy copy = getCopyOrThrow(copyId);
        Customer customer = customerService.getCustomerById(customerId);

        // Reserved copy checkout
        if (copy.getStatus() == CopyStatus.RESERVED) {
            return checkoutReservedCopy(copy, customer);
        }

        // Direct checkout
        if (copy.getStatus() == CopyStatus.AVAILABLE) {
            return checkoutAvailableCopy(copy, customer);
        }

        log.info("Copy with ID: {} is not available for checkout, current status: {}", copyId, copy.getStatus());
        throw new IllegalStateException(Messages.COPY_UNAVAILABLE_FOR_CHECKOUT + copy.getStatus());
    }

    private Copy checkoutReservedCopy(Copy copy, Customer customer) {
        log.debug("Checking out reserved copy with ID: {} for customer ID: {}", copy.getId(), customer.getId());
        if (!copy.getCustomer().getId().equals(customer.getId())) {
            log.info("Copy with ID: {} is reserved for another customer ID: {}, cannot checkout", copy.getId(), copy.getCustomer().getId());
            throw new IllegalStateException(Messages.COPY_RESERVED_FOR_ANOTHER_CUSTOMER + copy.getCustomer().getId());
        }
        copy.setStatus(CopyStatus.BORROWED);
        Copy savedCopy = copyRepository.save(copy);
        log.info("Reserved copy with ID: {} checked out successfully for customer ID: {}", savedCopy.getId(), customer.getId());
        return savedCopy;
    }

    private Copy checkoutAvailableCopy(Copy copy, Customer customer) {
        log.debug("Checking out available copy with ID: {} for customer ID: {}", copy.getId(), customer.getId());
        copy.setCustomer(customer);
        copy.setStatus(CopyStatus.BORROWED);

        Book book = copy.getBook();
        bookService.updateAvailableCopies(book.getIsbn(), -1);

        Copy savedCopy = copyRepository.save(copy);
        log.info("Available copy with ID: {} checked out successfully for customer ID: {}", savedCopy.getId(), customer.getId());
        return savedCopy;
    }

    // Helpers

    public Copy getCopyOrThrow(Long id) {
        log.debug("Looking up copy by ID: {}", id);
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            log.warn("Copy not found with ID: {}", id);
            throw new ResourceNotFoundException(Messages.COPY_NOT_FOUND + id);
        }
        Copy savedCopy = optionalCopy.get();
        log.debug("Found copy with ID: {}, status: {}", savedCopy.getId(), savedCopy.getStatus());
        return savedCopy;
    }
}
