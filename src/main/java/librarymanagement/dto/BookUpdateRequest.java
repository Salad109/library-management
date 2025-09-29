package librarymanagement.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import librarymanagement.constants.Messages;

import java.util.Set;

public record BookUpdateRequest(
        @NotBlank(message = Messages.BOOK_TITLE_VALIDATION_MESSAGE) String title,
        @Min(value = 1, message = Messages.BOOK_PUBLICATION_YEAR_VALIDATION_MESSAGE) Integer publicationYear,
        Set<@NotBlank String> authorNames
) {
}
