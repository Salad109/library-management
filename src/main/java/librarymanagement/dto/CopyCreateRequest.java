package librarymanagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import librarymanagement.constants.Messages;

public record CopyCreateRequest(
        @Pattern(regexp = Messages.BOOK_ISBN_REGEX, message = Messages.BOOK_ISBN_VALIDATION_MESSAGE) String bookIsbn,
        @Min(value = 1, message = Messages.COPY_MINIMUM_QUANTITY_VALIDATION_MESSAGE)
        @Max(value = 100, message = Messages.COPY_MAXIMUM_QUANTITY_VALIDATION_MESSAGE)
        Integer quantity) {
    public CopyCreateRequest {
        if (quantity == null) {
            quantity = 1;
        }
    }
}
