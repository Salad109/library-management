package librarymanagement.books;

import librarymanagement.SortFields;
import librarymanagement.authors.Author;
import librarymanagement.authors.AuthorRepository;
import librarymanagement.constants.Messages;
import librarymanagement.copies.CopyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class BookService {
    private static final Set<String> SORT_FIELDS = Set.of("title", "isbn", "publicationYear");
    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final CopyRepository copyRepository;

    public BookService(BookRepository bookRepository, AuthorRepository authorRepository, CopyRepository copyRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.copyRepository = copyRepository;
    }

    @Cacheable(value = "book-pages", key = "#pageable.pageNumber + '_' + #pageable.pageSize + '_' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooks(Pageable pageable) {
        SortFields.requireAllowed(pageable, SORT_FIELDS);
        Pageable stablePageable = SortFields.withTiebreaker(pageable, "isbn");
        return bookRepository.findAll(stablePageable).map(BookResponse::from);
    }

    @Cacheable(value = "books", key = "#isbn")
    @Transactional(readOnly = true)
    public BookResponse getBookByIsbn(String isbn) {
        return BookResponse.from(getBookOrThrow(isbn));
    }

    @Transactional(readOnly = true)
    public Page<BookResponse> searchBooks(String searchTerm, Pageable pageable) {
        SortFields.requireAllowed(pageable, SORT_FIELDS);
        if (searchTerm == null || searchTerm.isBlank()) {
            return Page.empty(pageable);
        }

        String cleanTerm = searchTerm.trim();
        Pageable stablePageable = SortFields.withTiebreaker(pageable, "isbn");
        return bookRepository.findByTitleOrAuthorContaining(cleanTerm, stablePageable).map(BookResponse::from);
    }

    @CachePut(value = "books", key = "#result.isbn")
    @Caching(evict = {
            @CacheEvict(value = "book-pages", allEntries = true),
            @CacheEvict(value = "authors", allEntries = true)})
    @Transactional
    public BookResponse addBook(BookCreateRequest bookCreateRequest) {
        if (bookRepository.existsByIsbn(bookCreateRequest.isbn())) {
            log.warn("Attempted to add duplicate book with ISBN: {}", bookCreateRequest.isbn());
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.BOOK_DUPLICATE + bookCreateRequest.isbn());
        }

        Book book = new Book();
        book.setIsbn(bookCreateRequest.isbn());
        book.setTitle(bookCreateRequest.title());
        book.setPublicationYear(bookCreateRequest.publicationYear());

        Set<Author> resolvedAuthors = resolveAuthors(bookCreateRequest.authorNames());
        book.setAuthors(resolvedAuthors);

        Book savedBook = bookRepository.save(book);
        log.info("Successfully added book: '{}' (ISBN: {})", savedBook.getTitle(), savedBook.getIsbn());

        return BookResponse.from(savedBook);
    }

    @CachePut(value = "books", key = "#isbn")
    @Caching(evict = {
            @CacheEvict(value = "book-pages", allEntries = true),
            @CacheEvict(value = "authors", allEntries = true)})
    @Transactional
    public BookResponse updateBook(String isbn, BookUpdateRequest bookUpdateRequest) {
        Book existingBook = getBookOrThrow(isbn);
        Set<Author> resolvedAuthors = resolveAuthors(bookUpdateRequest.authorNames());
        existingBook.setAuthors(resolvedAuthors);
        existingBook.setTitle(bookUpdateRequest.title());
        existingBook.setPublicationYear(bookUpdateRequest.publicationYear());

        Book savedBook = bookRepository.save(existingBook);

        log.info("Updated book with ISBN: {}", isbn);

        return BookResponse.from(savedBook);
    }

    // Cached books include availableCopies, so any copy change makes them stale
    @Caching(evict = {
            @CacheEvict(value = "books", key = "#isbn"),
            @CacheEvict(value = "book-pages", allEntries = true)})
    public void evictCachedBook(String isbn) {
    }

    @Caching(evict = {
            @CacheEvict(value = "books", key = "#isbn"),
            @CacheEvict(value = "book-pages", allEntries = true),
            @CacheEvict(value = "authors", allEntries = true)})
    @Transactional
    public void deleteBook(String isbn) {
        Book book = getBookOrThrow(isbn);
        if (copyRepository.existsByBookIsbn(isbn)) {
            log.warn("Attempted to delete book with copies, ISBN: {}", isbn);
            throw new ResponseStatusException(HttpStatus.CONFLICT, Messages.BOOK_HAS_COPIES + isbn);
        }

        bookRepository.delete(book);
        log.info("Deleted book: '{}' (ISBN: {})", book.getTitle(), isbn);
    }

    // Helpers

    public Book getBookOrThrow(String isbn) {
        return bookRepository.findByIsbnWithAuthors(isbn).orElseThrow(() -> {
            log.warn("Book not found with ISBN: {}", isbn);
            return new ResponseStatusException(HttpStatus.NOT_FOUND, Messages.BOOK_NOT_FOUND + isbn);
        });
    }

    private Set<Author> resolveAuthors(Set<String> authorNames) {
        Set<Author> authors = new LinkedHashSet<>();

        for (String authorName : authorNames) {
            Optional<Author> existingAuthor = authorRepository.findById(authorName);
            if (existingAuthor.isPresent()) {
                authors.add(existingAuthor.get());
            } else {
                Author savedAuthor = authorRepository.save(new Author(authorName));
                authors.add(savedAuthor);
            }
        }

        return authors;
    }
}