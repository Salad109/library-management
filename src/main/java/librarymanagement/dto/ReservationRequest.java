package librarymanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import librarymanagement.constants.Messages;

public record ReservationRequest(
        @NotNull Long customerId,

        @NotBlank
        @Pattern(
                regexp = Messages.BOOK_ISBN_REGEX,
                message = Messages.BOOK_ISBN_VALIDATION_MESSAGE
        )
        String bookIsbn
) {
}
