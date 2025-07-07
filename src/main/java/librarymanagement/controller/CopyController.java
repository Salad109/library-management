package librarymanagement.controller;

import jakarta.validation.Valid;
import librarymanagement.model.Book;
import librarymanagement.model.Copy;
import librarymanagement.model.CopyStatus;
import librarymanagement.service.BookService;
import librarymanagement.service.CopyService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class CopyController {

    private final CopyService copyService;
    private final BookService bookService;

    public CopyController(CopyService copyService, BookService bookService) {
        this.copyService = copyService;
        this.bookService = bookService;
    }

    @GetMapping("/api/copies")
    public Page<Copy> getAllCopies(Pageable pageable) {
        return copyService.getAllCopies(pageable);
    }

    @GetMapping("/api/copies/{id}")
    public Copy getCopyById(@PathVariable Long id) {
        return copyService.getCopyById(id);
    }

    @GetMapping("/api/copies/book/{isbn}")
    public Page<Copy> getCopiesByBookIsbn(@PathVariable String isbn, Pageable pageable) {
        return copyService.getCopiesByBookIsbn(isbn, pageable);
    }

    @GetMapping("/api/copies/book/{isbn}/available")
    public Page<Copy> getAvailableCopies(@PathVariable String isbn, Pageable pageable) {
        return copyService.getCopiesByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE, pageable);
    }

    @GetMapping("/api/copies/book/{isbn}/count")
    public long countAvailableCopies(@PathVariable String isbn) {
        return copyService.countCopiesByBookIsbnAndStatus(isbn, CopyStatus.AVAILABLE);
    }

    @PostMapping("/api/copies")
    @ResponseStatus(HttpStatus.CREATED)
    public Copy addCopy(@Valid @RequestBody Copy copy) {
        if (copy.getBook().getIsbn() == null) {
            throw new IllegalArgumentException("Book ISBN cannot be null");
        }

        // Make the copy associated with an existing book
        Book existingBook = bookService.getBookByIsbn(copy.getBook().getIsbn());
        copy.setBook(existingBook);

        return copyService.addCopy(copy);
    }

    @PutMapping("/api/copies/{copyId}/return/{customerId}")
    public Copy returnCopy(@PathVariable Long copyId, @PathVariable Long customerId) {
        return copyService.returnCopy(copyId, customerId);
    }

    @PutMapping("/api/copies/{copyId}/lost/{customerId}")
    public Copy markCopyAsLost(@PathVariable Long copyId, @PathVariable Long customerId) {
        return copyService.markCopyAsLost(copyId, customerId);
    }

    @PutMapping("/api/copies/{copyId}/undo-reserve/{customerId}")
    public Copy unreserveCopy(@PathVariable Long copyId, @PathVariable Long customerId) {
        return copyService.cancelCopyReservation(copyId, customerId);
    }

    @PutMapping("/api/copies/{copyId}/checkout")
    public Copy checkoutReservedCopy(@PathVariable Long copyId) {
        return copyService.checkoutReservedCopy(copyId);
    }

    @DeleteMapping("/api/copies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCopy(@PathVariable Long id) {
        copyService.deleteCopy(id);
    }
}
