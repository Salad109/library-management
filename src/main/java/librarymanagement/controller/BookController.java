package librarymanagement.controller;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import librarymanagement.exception.DuplicateResourceException;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Author;
import librarymanagement.model.Book;
import librarymanagement.repository.AuthorRepository;
import librarymanagement.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
public class BookController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorRepository authorRepository;

    @GetMapping("/api/books")
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @GetMapping("/api/books/{id}")
    public Book getBookById(@PathVariable Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with ID: " + id));
    }

    @GetMapping("/api/books/search")
    public List<Book> searchBooks(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String authorName,
            @RequestParam(required = false) Integer publicationYear,
            @RequestParam(required = false) String isbn
    ) {
        // If no parameters provided, return all books
        if (title == null && authorName == null && publicationYear == null && isbn == null) {
            return bookRepository.findAll();
        } else {
            return bookRepository.searchBooks(title, authorName, publicationYear, isbn);
        }
    }

    @PostMapping("/api/books")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Book addBook(@Valid @RequestBody Book book) {
        if (bookRepository.existsByIsbn(book.getIsbn())) {
            throw new DuplicateResourceException("A book with this ISBN already exists: " + book.getIsbn());
        }

        book.setAuthors(deduplicateAuthors(book.getAuthors()));
        return bookRepository.save(book);
    }

    @PutMapping("/api/books/{id}")
    @Transactional
    public Book updateBook(@PathVariable Long id, @Valid @RequestBody Book book) {
        Book existingBook = bookRepository.findById(id).orElse(null);
        if (existingBook == null) {
            throw new ResourceNotFoundException("Book not found with ID: " + id);
        }
        // Check if the new ISBN is already taken by another book
        if (bookRepository.existsByIsbn(book.getIsbn()) && !existingBook.getIsbn().equals(book.getIsbn())) {
            throw new DuplicateResourceException("A book with this ISBN already exists: " + book.getIsbn());
        }
        existingBook.setAuthors(deduplicateAuthors(book.getAuthors()));
        existingBook.setTitle(book.getTitle());
        existingBook.setPublicationYear(book.getPublicationYear());
        existingBook.setIsbn(book.getIsbn());
        return bookRepository.save(existingBook);
    }

    @DeleteMapping("/api/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteBook(@PathVariable Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book not found with id: " + id);
        }
        bookRepository.deleteById(id);
    }

    // Helper
    private Set<Author> deduplicateAuthors(Set<Author> incomingAuthors) {
        Set<Author> finalAuthors = new HashSet<>();
        for (Author incomingAuthor : incomingAuthors) {
            Author existingAuthor = authorRepository.findByName(incomingAuthor.getName()); // Check if the author already exists in the database
            if (existingAuthor != null) {
                finalAuthors.add(existingAuthor); // if found, use existing author
            } else {
                finalAuthors.add(incomingAuthor); // else, add the new author
            }
        }
        return finalAuthors;
    }
}
