package librarymanagement.controller;

import librarymanagement.model.Book;
import librarymanagement.model.CopyStatus;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class BookController {

    private final BookService bookService;
    private final CopyService copyService;

    public BookController(BookService bookService, CopyService copyService) {
        this.bookService = bookService;
        this.copyService = copyService;
    }

    @GetMapping("/api/books")
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookService.getAllBooks(pageable);
    }

    @GetMapping("/api/books/{isbn}")
    public Book getBookByIsbn(@PathVariable String isbn) {
        return bookService.getBookByIsbn(isbn);
    }

    @GetMapping("/api/books/{isbn}/count")
    public long countAvailableCopies(@PathVariable String isbn) {
        return copyService.countCopiesByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE);
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
}
