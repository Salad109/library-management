package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Book;
import librarymanagement.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
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
    @CacheEvict(value = "book-pages", allEntries = true)
    public Book addBook(Book book) {
        String authorNames = book.getFormattedAuthors();
        log.info("Adding book: '{}' by [{}] (ISBN: {})", book.getTitle(), authorNames, book.getIsbn());

        if (bookRepository.existsByIsbn(book.getIsbn())) {
            log.warn("Attempted to add duplicate book with ISBN: {}", book.getIsbn());
            throw new DuplicateResourceException(Messages.BOOK_DUPLICATE + book.getIsbn());
        }

        Book savedBook = bookRepository.save(book);
        log.info("Successfully added book: '{}' (ISBN: {})", savedBook.getTitle(), savedBook.getIsbn());

        return savedBook;
    }

    @CachePut(value = "books", key = "#isbn")
    @CacheEvict(value = "book-pages", allEntries = true)
    @Transactional
    public Book updateBook(String isbn, Book book) {
        log.info("Updating book with ISBN: {}", isbn);

        Optional<Book> optionalBook = bookRepository.findById(isbn);
        if (optionalBook.isEmpty()) {
            log.warn("Attempted to update non-existent book with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }

        Book existingBook = optionalBook.get();
        String oldTitle = existingBook.getTitle();
        String oldAuthors = existingBook.getFormattedAuthors();

        existingBook.setAuthors(book.getAuthors());
        existingBook.setTitle(book.getTitle());
        existingBook.setPublicationYear(book.getPublicationYear());

        Book savedBook = bookRepository.save(existingBook);

        log.info("Updated book (ISBN: {}): '{}' by [{}] to '{}' by [{}]",
                isbn, oldTitle, oldAuthors,
                savedBook.getTitle(), savedBook.getFormattedAuthors());

        return savedBook;
    }

    @Caching(evict = {@CacheEvict(value = "books", key = "#isbn"), @CacheEvict(value = "book-pages", allEntries = true)})
    @Transactional
    public void deleteBook(String isbn) {
        log.info("Deleting book with ISBN: {}", isbn);

        Optional<Book> book = bookRepository.findById(isbn);
        if (book.isEmpty()) {
            log.warn("Attempted to delete non-existent book with ISBN: {}", isbn);
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }

        String title = book.get().getTitle();

        bookRepository.deleteById(isbn);
        log.info("Successfully deleted book: '{}' (ISBN: {})", title, isbn);
    }
}