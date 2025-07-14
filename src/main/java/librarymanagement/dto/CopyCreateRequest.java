package librarymanagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import librarymanagement.constants.Messages;

public record CopyCreateRequest(
        @Pattern(regexp = Messages.BOOK_ISBN_REGEX, message = Messages.BOOK_ISBN_VALIDATION_MESSAGE) String bookIsbn,
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100, message = "Cannot create more than 100 copies at once")
        Integer quantity) {
    public CopyCreateRequest {
        if (quantity == null) {
            quantity = 1;
        }
    }
}
