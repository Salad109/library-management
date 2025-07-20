package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.model.Customer;
import librarymanagement.repository.BookRepository;
import librarymanagement.repository.CopyRepository;
import librarymanagement.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CopyService {

    private static final Logger log = LoggerFactory.getLogger(CopyService.class);
    private final CopyRepository copyRepository;
    private final BookRepository bookRepository;
    private final CustomerRepository customerRepository;

    public CopyService(CopyRepository copyRepository, BookRepository bookRepository, CustomerRepository customerRepository) {
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
        this.customerRepository = customerRepository;
    }

    public Page<Copy> getAllCopies(Pageable pageable) {
        log.debug("Fetching all copies, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Copy> copies = copyRepository.findAll(pageable);
        log.debug("Retrieved {} copies out of {} total", copies.getNumberOfElements(), copies.getTotalElements());
        return copies;
    }

    public Copy getCopyById(Long id) {
        return getCopyOrThrow(id);
    }

    public Page<Copy> getCopiesByBookIsbn(String isbn, Pageable pageable) {
        log.debug("Fetching copies for book ISBN: {}, page: {}, size: {}", isbn, pageable.getPageNumber(), pageable.getPageSize());
        Page<Copy> copies = copyRepository.findByBookIsbn(isbn, pageable);
        log.debug("Retrieved {} copies for ISBN: {} out of {} total", copies.getNumberOfElements(), isbn, copies.getTotalElements());
        return copies;
    }

    public Page<Copy> getCopiesByCustomerId(Long customerId, Pageable pageable) {
        log.debug("Fetching copies for customer ID: {}, page: {}, size: {}", customerId, pageable.getPageNumber(), pageable.getPageSize());
        Page<Copy> copies = copyRepository.findByCustomerId(customerId, pageable);
        log.debug("Retrieved {} copies for customer ID: {} out of {} total", copies.getNumberOfElements(), customerId, copies.getTotalElements());
        return copies;
    }

    public long countCopiesByBookIsbnAndStatus(String isbn, CopyStatus status) {
        log.debug("Counting copies for book ISBN: {} with status: {}", isbn, status);
        long count = copyRepository.countByBookIsbnAndStatus(isbn, status);
        log.debug("Found {} copies for ISBN: {} with status: {}", count, isbn, status);
        return count;
    }

    @Transactional
    public List<Copy> addCopies(String isbn, int quantity) {
        log.info("Adding {} copies for book with ISBN: {}", quantity, isbn);
        Optional<Book> book = bookRepository.findByIsbn(isbn);
        if (book.isEmpty()) {
            log.debug("Book not found with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }
        Book existingBook = book.get();

        List<Copy> copies = new ArrayList<>(quantity);
        for (int i = 0; i < quantity; i++) {
            Copy copy = new Copy();
            copy.setBook(existingBook);
            copy.setStatus(CopyStatus.AVAILABLE);
            copies.add(copy);
        }
        List<Copy> savedCopies = copyRepository.saveAll(copies);
        log.info("Added {} copies for book with ISBN: {}", savedCopies.size(), isbn);
        return savedCopies;
    }

    @Transactional
    public Copy returnCopy(Long copyId, Long customerId) {
        log.info("Returning copy with ID: {} for customer ID: {}", copyId, customerId);
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.BORROWED) {
            log.debug("Copy with ID: {} is not borrowed, current status: {}", copyId, existingCopy.getStatus());
            throw new IllegalStateException(Messages.COPY_NOT_BORROWED + existingCopy.getStatus());
        }

        Customer customer = getCustomerOrThrow(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            log.debug("Copy with ID: {} is reserved for another customer, current customer ID: {}", copyId, existingCopy.getCustomer().getId());
            throw new IllegalStateException(Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setCustomer(null);
        existingCopy.setStatus(CopyStatus.AVAILABLE);
        Copy savedCopy = copyRepository.save(existingCopy);
        log.info("Copy with ID: {} returned successfully", savedCopy.getId());
        return savedCopy;
    }

    @Transactional
    public Copy markLost(Long copyId) {
        log.info("Marking copy with ID: {} as lost", copyId);
        Copy existingCopy = getCopyOrThrow(copyId);

        existingCopy.setStatus(CopyStatus.LOST);
        Copy savedCopy = copyRepository.save(existingCopy);
        log.info("Copy with ID: {} marked as lost", savedCopy.getId());
        return savedCopy;
    }

    @Transactional
    public Copy reserveAnyAvailableCopy(String isbn, Long customerId) {
        log.info("Reserving any available copy for book with ISBN: {} for customer ID: {}", isbn, customerId);
        Page<Copy> availableCopies = copyRepository.findByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE, Pageable.unpaged());
        if (availableCopies.isEmpty()) {
            log.debug("No available copies found for book with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.COPY_NO_AVAILABLE + isbn);
        }

        Copy copyToReserve = availableCopies.getContent().get(0); // Reserve the first available copy
        log.info("Reserving copy with ID: {} for customer ID: {}", copyToReserve.getId(), customerId);
        return reserveCopy(copyToReserve.getId(), customerId);
    }

    @Transactional
    public Copy reserveCopy(Long copyId, Long customerId) {
        Copy existingCopy = getCopyOrThrow(copyId);
        Customer customer = getCustomerOrThrow(customerId);

        existingCopy.setCustomer(customer);
        existingCopy.setStatus(CopyStatus.RESERVED);
        Copy savedCopy = copyRepository.save(existingCopy);
        log.info("Copy with ID: {} reserved for customer ID: {}", copyId, customerId);
        return savedCopy;
    }

    @Transactional
    public Copy cancelReservation(Long copyId, Long customerId) {
        log.info("Cancelling reservation for copy with ID: {} for customer ID: {}", copyId, customerId);
        Copy existingCopy = getCopyOrThrow(copyId);
        if (existingCopy.getStatus() != CopyStatus.RESERVED) {
            log.debug("Copy with ID: {} is not reserved, current status: {}", copyId, existingCopy.getStatus());
            throw new IllegalStateException(Messages.COPY_NOT_RESERVED + existingCopy.getStatus());
        }

        Customer customer = getCustomerOrThrow(customerId);
        if (existingCopy.getCustomer() != null && !existingCopy.getCustomer().equals(customer)) {
            log.debug("Copy with ID: {} is reserved for customer ID: {}, not for customer ID: {}", copyId, existingCopy.getCustomer().getId(), customerId);
            throw new IllegalStateException(Messages.COPY_WRONG_CUSTOMER + existingCopy.getCustomer().getId());
        }

        existingCopy.setCustomer(null);
        existingCopy.setStatus(CopyStatus.AVAILABLE);
        Copy savedCopy = copyRepository.save(existingCopy);
        log.info("Reservation for copy with ID: {} cancelled successfully", savedCopy.getId());
        return savedCopy;
    }

    @Transactional
    public Copy checkout(Long copyId, Long customerId) {
        log.info("Checking out copy with ID: {} for customer ID: {}", copyId, customerId);
        Copy copy = getCopyOrThrow(copyId);
        Customer customer = getCustomerOrThrow(customerId);

        // Reserved copy checkout
        if (copy.getStatus() == CopyStatus.RESERVED) {
            return checkoutReservedCopy(copy, customer);
        }

        // Direct checkout
        if (copy.getStatus() == CopyStatus.AVAILABLE) {
            return checkoutAvailableCopy(copy, customer);
        }

        log.debug("Copy with ID: {} is not available for checkout, current status: {}", copyId, copy.getStatus());
        throw new IllegalStateException(Messages.COPY_UNAVAILABLE_FOR_CHECKOUT + copy.getStatus());
    }

    private Copy checkoutReservedCopy(Copy copy, Customer customer) {
        log.info("Checking out reserved copy with ID: {} for customer ID: {}", copy.getId(), customer.getId());
        if (!copy.getCustomer().getId().equals(customer.getId())) {
            log.debug("Copy with ID: {} is reserved for another customer ID: {}, cannot checkout", copy.getId(), copy.getCustomer().getId());
            throw new IllegalStateException(Messages.COPY_RESERVED_FOR_ANOTHER_CUSTOMER + copy.getCustomer().getId());
        }
        copy.setStatus(CopyStatus.BORROWED);
        Copy savedCopy = copyRepository.save(copy);
        log.info("Reserved copy with ID: {} checked out successfully for customer ID: {}", savedCopy.getId(), customer.getId());
        return savedCopy;
    }

    private Copy checkoutAvailableCopy(Copy copy, Customer customer) {
        log.info("Checking out available copy with ID: {} for customer ID: {}", copy.getId(), customer.getId());
        copy.setCustomer(customer);
        copy.setStatus(CopyStatus.BORROWED);
        Copy savedCopy = copyRepository.save(copy);
        log.info("Available copy with ID: {} checked out successfully for customer ID: {}", savedCopy.getId(), customer.getId());
        return savedCopy;
    }

    // Helpers

    public Copy getCopyOrThrow(Long id) {
        log.debug("Looking up copy by ID: {}", id);
        Optional<Copy> optionalCopy = copyRepository.findById(id);
        if (optionalCopy.isEmpty()) {
            log.debug("Copy not found with ID: {}", id);
            throw new ResourceNotFoundException(Messages.COPY_NOT_FOUND + id);
        }
        Copy savedCopy = optionalCopy.get();
        log.debug("Found copy with ID: {}, status: {}", savedCopy.getId(), savedCopy.getStatus());
        return savedCopy;
    }

    public Customer getCustomerOrThrow(Long customerId) {
        log.debug("Looking up customer by ID: {}", customerId);
        Optional<Customer> optionalCustomer = customerRepository.findById(customerId);
        if (optionalCustomer.isEmpty()) {
            log.debug("Customer not found with ID: {}", customerId);
            throw new ResourceNotFoundException(Messages.CUSTOMER_NOT_FOUND + customerId);
        }
        Customer customer = optionalCustomer.get();
        log.debug("Found customer: {} {} with ID: {}", customer.getFirstName(), customer.getLastName(), customerId);
        return customer;
    }
}
