package librarymanagement.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Public Browsing")
public class BookController {

    private final BookService bookService;
    private final CopyService copyService;

    public BookController(BookService bookService, CopyService copyService) {
        this.bookService = bookService;
        this.copyService = copyService;
    }

    @Operation(summary = "Get all books")
    @ApiResponse(responseCode = "200", description = "Books retrieved successfully")
    @GetMapping("/api/books")
    public Page<Book> getAllBooks(Pageable pageable) {
        return bookService.getAllBooks(pageable);
    }

    @Operation(summary = "Get book by ISBN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book found"),
            @ApiResponse(responseCode = "404", description = "Book not found with the given ISBN")
    })
    @GetMapping("/api/books/{isbn}")
    public Book getBookByIsbn(@PathVariable String isbn) {
        return bookService.getBookByIsbn(isbn);
    }

    @Operation(summary = "Count available copies of a book by ISBN")
    @ApiResponse(responseCode = "200", description = "Count of available copies retrieved successfully")
    @GetMapping("/api/books/{isbn}/count")
    public long countAvailableCopies(@PathVariable String isbn) {
        return copyService.countCopiesByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE);
    }

    @Operation(summary = "Search books by various criteria",
            description = "Search by title, author name, publication year, or ISBN. " +
                    "Any parameter can be null or blank to ignore that criterion.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Books found matching the search criteria"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
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
