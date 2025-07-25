package librarymanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import librarymanagement.constants.Messages;

public record CopyReservationRequest(
        @NotBlank(message = Messages.BOOK_NULL_ISBN)
        @Pattern(
                regexp = Messages.BOOK_ISBN_REGEX,
                message = Messages.BOOK_ISBN_VALIDATION_MESSAGE
        )
        String bookIsbn
) {
}
