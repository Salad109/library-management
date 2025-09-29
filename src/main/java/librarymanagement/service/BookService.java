package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.dto.BookCreateRequest;
import librarymanagement.dto.BookUpdateRequest;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Author;
import librarymanagement.model.Book;
import librarymanagement.repository.AuthorRepository;
import librarymanagement.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
    }

    @Cacheable(value = "book-pages", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
    public Page<Book> getAllBooks(Pageable pageable) {
        log.debug("Fetching all books, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        Page<String> isbnPage = bookRepository.findAllIsbns(pageable);

        if (isbnPage.getContent().isEmpty()) {
            log.debug("No books found, returning empty page");
            return Page.empty(pageable);
        }

        List<Book> books = bookRepository.findByIsbnsWithAuthors(isbnPage.getContent());

        log.debug("Retrieved {} books out of {} total", books.size(), isbnPage.getTotalElements());

        return new PageImpl<>(books, pageable, isbnPage.getTotalElements());
    }

    @Cacheable(value = "books", key = "#isbn")
    public Book getBookByIsbn(String isbn) {
        log.debug("Looking up book by ISBN: {}", isbn);
        Optional<Book> book = bookRepository.findByIsbnWithAuthors(isbn);
        if (book.isEmpty()) {
            log.warn("Book not found with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }
        log.debug("Found book: '{}'", book.get().getTitle());
        return book.get();
    }

    public Page<Book> searchBooks(String searchTerm, Pageable pageable) {
        log.debug("Searching books with term: '{}', page: {}, size: {}", searchTerm, pageable.getPageNumber(), pageable.getPageSize());

        if (searchTerm == null || searchTerm.isBlank()) {
            return Page.empty(pageable);
        }

        String cleanTerm = searchTerm.trim();

        Page<String> titleIsbns = bookRepository.findIsbnsByTitleContaining(cleanTerm, pageable);

        if (titleIsbns.getTotalElements() > 0) {
            log.debug("Found {} books matching title search for term: '{}'", titleIsbns.getTotalElements(), cleanTerm);

            List<Book> books = bookRepository.findByIsbnsWithAuthors(titleIsbns.getContent());
            return new PageImpl<>(books, pageable, titleIsbns.getTotalElements());
        }

        Page<String> authorIsbns = bookRepository.findIsbnsByAuthorContaining(cleanTerm, pageable);
        log.debug("No books found matching title search for term: '{}'. Returning {} books by authors",
                cleanTerm, authorIsbns.getTotalElements());

        if (authorIsbns.getContent().isEmpty()) {
            return Page.empty(pageable);
        }

        List<Book> books = bookRepository.findByIsbnsWithAuthors(authorIsbns.getContent());
        return new PageImpl<>(books, pageable, authorIsbns.getTotalElements());
    }

    @CachePut(value = "books", key = "#result.isbn")
    @Caching(evict = {
            @CacheEvict(value = "book-pages", allEntries = true),
            @CacheEvict(value = "authors", allEntries = true)})
    @Retryable(retryFor = DataIntegrityViolationException.class, backoff = @Backoff(delay = 50), maxAttempts = 2)
    public Book addBook(BookCreateRequest bookCreateRequest) {
        String formattedAuthors = String.join(", ", bookCreateRequest.authorNames());
        log.debug("Adding book: '{}' by [{}] (ISBN: {})", bookCreateRequest.title(), formattedAuthors, bookCreateRequest.isbn());

        if (bookRepository.existsByIsbn(bookCreateRequest.isbn())) {
            log.warn("Attempted to add duplicate book with ISBN: {}", bookCreateRequest.isbn());
            throw new DuplicateResourceException(Messages.BOOK_DUPLICATE + bookCreateRequest.isbn());
        }

        Book book = new Book();
        book.setIsbn(bookCreateRequest.isbn());
        book.setTitle(bookCreateRequest.title());
        book.setPublicationYear(bookCreateRequest.publicationYear());

        Set<Author> resolvedAuthors = resolveAuthors(bookCreateRequest.authorNames());
        book.setAuthors(resolvedAuthors);

        Book savedBook = bookRepository.save(book);
        log.info("Successfully added book: '{}' (ISBN: {})", savedBook.getTitle(), savedBook.getIsbn());

        return savedBook;
    }

    @CachePut(value = "books", key = "#isbn")
    @Caching(evict = {
            @CacheEvict(value = "book-pages", allEntries = true),
            @CacheEvict(value = "authors", allEntries = true)})
    @Transactional
    public Book updateBook(String isbn, BookUpdateRequest bookUpdateRequest) {
        log.debug("Updating book with ISBN: {}", isbn);

        Optional<Book> optionalBook = bookRepository.findById(isbn);
        if (optionalBook.isEmpty()) {
            log.warn("Attempted to update non-existent book with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }

        Book existingBook = optionalBook.get();
        String oldTitle = existingBook.getTitle();
        String oldAuthors = existingBook.getFormattedAuthors();
        int oldYear = existingBook.getPublicationYear();

        Set<Author> resolvedAuthors = resolveAuthors(bookUpdateRequest.authorNames());
        existingBook.setAuthors(resolvedAuthors);
        existingBook.setTitle(bookUpdateRequest.title());
        existingBook.setPublicationYear(bookUpdateRequest.publicationYear());

        Book savedBook = bookRepository.save(existingBook);

        log.info("Updated book (ISBN: {}): '{}' {} by [{}] to '{}' {} by [{}]",
                isbn, oldTitle, oldYear, oldAuthors,
                savedBook.getTitle(), savedBook.getPublicationYear(), savedBook.getFormattedAuthors());

        return savedBook;
    }

    @CachePut(value = "books", key = "#isbn")
    @CacheEvict(value = "book-pages", allEntries = true)
    @Transactional
    @Retryable(retryFor = OptimisticLockingFailureException.class, backoff = @Backoff(delay = 50))
    public Book updateAvailableCopies(String isbn, int delta) {
        log.debug("Updating available copies for ISBN: {} by delta: {}", isbn, delta);

        Optional<Book> optionalBook = bookRepository.findById(isbn);
        if (optionalBook.isEmpty()) {
            log.warn("Attempted to update copies for non-existent book with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }

        Book book = optionalBook.get();
        int newCount = book.getAvailableCopies() + delta;
        if (newCount < 0) {
            log.warn("Available copies would become negative for ISBN: {}", isbn);
            throw new IllegalStateException("Available copies cannot be negative");
        }

        book.setAvailableCopies(newCount);
        Book savedBook = bookRepository.save(book);

        log.debug("Updated available copies for ISBN: {} to {}", isbn, newCount);
        return savedBook;
    }

    @Caching(evict = {
            @CacheEvict(value = "books", key = "#isbn"),
            @CacheEvict(value = "book-pages", allEntries = true),
            @CacheEvict(value = "authors", allEntries = true)})
    @Transactional
    public void deleteBook(String isbn) {
        log.debug("Deleting book with ISBN: {}", isbn);

        Optional<Book> book = bookRepository.findById(isbn);
        if (book.isEmpty()) {
            log.warn("Attempted to delete non-existent book with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }

        String title = book.get().getTitle();

        bookRepository.deleteById(isbn);
        log.info("Successfully deleted book: '{}' (ISBN: {})", title, isbn);
    }

    private Set<Author> resolveAuthors(Set<String> authorNames) {
        Set<Author> authors = new LinkedHashSet<>();

        for (String authorName : authorNames) {
            Optional<Author> existingAuthor = authorRepository.findById(authorName);
            if (existingAuthor.isPresent()) {
                log.debug("Using existing author: {}", authorName);
                authors.add(existingAuthor.get());
            } else {
                log.debug("Creating new author: {}", authorName);
                Author savedAuthor = authorRepository.save(new Author(authorName));
                authors.add(savedAuthor);
            }
        }

        return authors;
    }
}