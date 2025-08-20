package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Book;
import librarymanagement.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<Book> getAllBooks(Pageable pageable) {
        log.debug("Fetching all books, page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Book> books = bookRepository.findAll(pageable);
        log.debug("Retrieved {} books out of {} total", books.getNumberOfElements(), books.getTotalElements());
        return books;
    }

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
            log.debug("Search term is empty or null, returning all books");
            return bookRepository.findAll(pageable);
        }

        String cleanTerm = searchTerm.trim();

        Page<Book> titleResults = bookRepository.findByTitleContaining(cleanTerm, pageable);

        if (titleResults.getTotalElements() != 0) {
            log.debug("Found {} books matching title search for term: '{}'", titleResults.getNumberOfElements(), cleanTerm);
            return titleResults;
        } else {
            Page<Book> authorResults = bookRepository.findByAuthorContaining(cleanTerm, pageable);
            log.debug("No books found matching title search for term: '{}'. Returning {} books by authors",
                    cleanTerm, authorResults.getNumberOfElements());
            return authorResults;
        }
    }

    public Book addBook(Book book) {
        String authorNames = book.getFormattedAuthors();
        log.info("Adding book: '{}' by [{}] (ISBN: {})",
                book.getTitle(), authorNames, book.getIsbn());

        if (bookRepository.findByIsbnWithAuthors(book.getIsbn()).isPresent()) {
            log.warn("Attempted to add duplicate book with ISBN: {}", book.getIsbn());
            throw new DuplicateResourceException(Messages.BOOK_DUPLICATE + book.getIsbn());
        }

        Book savedBook = bookRepository.save(book);
        log.info("Successfully added book: '{}' (ISBN: {})",
                savedBook.getTitle(), savedBook.getIsbn());

        return savedBook;
    }

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