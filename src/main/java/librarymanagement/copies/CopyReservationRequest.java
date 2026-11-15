package librarymanagement.copies;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import librarymanagement.books.Book;

public record CopyReservationRequest(
        @NotBlank @Pattern(regexp = Book.ISBN_REGEX, message = Book.ISBN_INVALID) String bookIsbn
) {
}
