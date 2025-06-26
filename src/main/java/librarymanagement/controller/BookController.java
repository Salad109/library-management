package librarymanagement.controller;

import io.undertow.util.BadRequestException;
import jakarta.validation.Valid;
import librarymanagement.exception.ResourceNotFoundException;
import librarymanagement.model.Book;
import librarymanagement.service.BookService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


@RestController
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping("/api/books")
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookService.getAllBooks(pageable);
    }

    @GetMapping("/api/books/{isbn}")
    public Book getBookByIsbn(@PathVariable String isbn) {
        return bookService.getBookByIsbn(isbn);
    }

    @GetMapping("/api/books/search")
    public Page<Book> searchBooks(@RequestParam(required = false) String title, @RequestParam(required = false) String authorName, @RequestParam(required = false) Integer publicationYear, @RequestParam(required = false) String isbn, Pageable pageable) {
        title = nullBlankString(title);
        authorName = nullBlankString(authorName);
        isbn = nullBlankString(isbn);

        return bookService.searchBooks(title, authorName, publicationYear, isbn, pageable);
    }

    private String nullBlankString(String str) {
        if (str != null && str.isBlank())
            return null;
        else
            return str;
    }

    @PostMapping("/api/books")
    @ResponseStatus(HttpStatus.CREATED)
    public Book addBook(@Valid @RequestBody Book book) {
        return bookService.addBook(book);
    }

    @PutMapping("/api/books/{isbn}")
    public Book updateBook(@PathVariable String isbn, @Valid @RequestBody Book book) throws BadRequestException, ResourceNotFoundException {
        if (!isbn.equals(book.getIsbn())) {
            throw new BadRequestException("Cannot change ISBN of an existing book");
        }

        return bookService.updateBook(isbn, book);
    }

    @DeleteMapping("/api/books/{isbn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable String isbn) {
        bookService.deleteBook(isbn);
    }
}
