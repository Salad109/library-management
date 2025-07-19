package librarymanagement.service;

import jakarta.transaction.Transactional;
import librarymanagement.constants.Messages;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Book;
import librarymanagement.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    public Book getBookByIsbn(String isbn) {
        Optional<Book> book = bookRepository.findByIsbn(isbn);
        if (book.isEmpty()) {
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }
        return book.get();
    }

    public Page<Book> searchBooks(String title, String authorName, Integer publicationYear, String isbn, Pageable pageable) {
        return bookRepository.searchBooks(title, authorName, publicationYear, isbn, pageable);
    }

    public Book addBook(Book book) {
        if (bookRepository.findByIsbn(book.getIsbn()).isPresent()) {
            throw new DuplicateResourceException(Messages.BOOK_DUPLICATE + book.getIsbn());
        }
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(String isbn, Book book) {
        Optional<Book> optionalBook = bookRepository.findById(isbn);
        if (optionalBook.isEmpty()) {
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }

        Book existingBook = optionalBook.get();

        existingBook.setAuthors(book.getAuthors());
        existingBook.setTitle(book.getTitle());
        existingBook.setPublicationYear(book.getPublicationYear());
        return bookRepository.save(existingBook);
    }

    @Transactional
    public void deleteBook(String isbn) {
        if (!bookRepository.existsById(isbn)) {
            throw new ResourceNotFoundException(Messages.BOOK_NOT_FOUND + isbn);
        }
        bookRepository.deleteById(isbn);
    }
}
