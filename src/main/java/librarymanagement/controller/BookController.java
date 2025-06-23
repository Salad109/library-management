package librarymanagement.controller;

import io.undertow.util.BadRequestException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Book;
import librarymanagement.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @GetMapping("/api/books")
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @GetMapping("/api/books/{isbn}")
    public Book getBookByIsbn(@PathVariable String isbn) {
        Book book = bookRepository.findByIsbn(isbn);
        if (book == null) {
            throw new ResourceNotFoundException("Book not found with ISBN: " + isbn);
        }
        return book;
    }

    @GetMapping("/api/books/search")
    public Page<Book> searchBooks(@RequestParam(required = false) String title, @RequestParam(required = false) String authorName, @RequestParam(required = false) Integer publicationYear, @RequestParam(required = false) String isbn, Pageable pageable) {
        // If no parameters provided, return all books
        if (title == null && authorName == null && publicationYear == null && isbn == null) {
            return bookRepository.findAll(pageable);
        } else {
            return bookRepository.searchBooks(title, authorName, publicationYear, isbn, pageable);
        }
    }

    @PostMapping("/api/books")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Book addBook(@Valid @RequestBody Book book) {
        if (bookRepository.findByIsbn(book.getIsbn()) != null) {
            throw new DuplicateResourceException("A book with this ISBN already exists: " + book.getIsbn());
        }

        return bookRepository.save(book);
    }

    @PutMapping("/api/books/{isbn}")
    @Transactional
    public Book updateBook(@PathVariable String isbn, @Valid @RequestBody Book book) throws BadRequestException {
        Optional<Book> optionalBook = bookRepository.findById(isbn);
        if (optionalBook.isEmpty()) {
            throw new ResourceNotFoundException("Book not found with ISBN: " + isbn);
        }

        Book existingBook = optionalBook.get();

        if (!existingBook.getIsbn().equals(book.getIsbn())) {
            throw new BadRequestException("Cannot change ISBN of an existing book");
        }

        existingBook.setAuthors(book.getAuthors());
        existingBook.setTitle(book.getTitle());
        existingBook.setPublicationYear(book.getPublicationYear());
        return bookRepository.save(existingBook);
    }

    @DeleteMapping("/api/books/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteBook(@PathVariable String isbn) {
        if (!bookRepository.existsById(isbn)) {
            throw new ResourceNotFoundException("Book not found with ISBN: " + isbn);
        }
        bookRepository.deleteById(isbn);
    }
}
