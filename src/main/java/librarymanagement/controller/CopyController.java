package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CopyController {

    private final CopyService copyService;
    private final BookService bookService;

    public CopyController(CopyService copyService, BookService bookService) {
        this.copyService = copyService;
        this.bookService = bookService;
    }

    @GetMapping("/api/copies")
    public List<Copy> getAllCopies() {
        return copyService.getAllCopies();
    }

    @GetMapping("/api/copies/book/{isbn}")
    public List<Copy> getCopiesByBookIsbn(@PathVariable String isbn) {
        return copyService.getCopiesByBookIsbn(isbn);
    }

    @GetMapping("/api/copies/book/{isbn}/available")
    public List<Copy> getAvailableCopies(@PathVariable String isbn) {
        return copyService.getCopiesByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE);
    }

    @GetMapping("/api/copies/book/{isbn}/count")
    public long countAvailableCopies(@PathVariable String isbn) {
        return copyService.countCopiesByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE);
    }

    @PostMapping("/api/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public Copy addCopy(@Valid @RequestBody Copy copy) {
        if (copy.getBook() == null || copy.getBook().getIsbn() == null) {
            throw new IllegalArgumentException("Copy must be associated with an existing book");
        }

        // Make the copy associated with an existing book
        Book existingBook = bookService.getBookByIsbn(copy.getBook().getIsbn());
        copy.setBook(existingBook);

        return copyService.addCopy(copy);
    }

    @PutMapping("/api/copies/{id}")
    public Copy updateCopy(@PathVariable Long id, @Valid @RequestBody Copy copy) {
        if (!id.equals(copy.getId())) {
            throw new IllegalArgumentException("Cannot change ID of an existing copy");
        }
        return copyService.updateCopy(id, copy);
    }

    @PutMapping("/api/copies/{id}/borrow")
    public Copy borrowCopy(@PathVariable Long id) {
        return copyService.borrowCopy(id);
    }

    @PutMapping("/api/copies/{id}/return")
    public Copy returnCopy(@PathVariable Long id) {
        return copyService.returnCopy(id);
    }

    @PutMapping("/api/copies/{id}/lost")
    public Copy markCopyAsLost(@PathVariable Long id) {
        return copyService.markCopyAsLost(id);
    }

    @DeleteMapping("/api/copies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCopy(@PathVariable Long id) {
        copyService.deleteCopy(id);
    }
}
