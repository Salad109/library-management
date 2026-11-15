package librarymanagement.copies;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import librarymanagement.books.Book;

public record CopyCreateRequest(
        @NotNull @Pattern(regexp = Book.ISBN_REGEX, message = Book.ISBN_INVALID) String bookIsbn,
        @Min(1) @Max(100) Integer quantity) {
    public CopyCreateRequest {
        if (quantity == null) {
            quantity = 1;
        }
    }
}
