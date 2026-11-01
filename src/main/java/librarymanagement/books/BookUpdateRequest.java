package librarymanagement.books;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import librarymanagement.constants.Messages;

import java.util.Set;

public record BookUpdateRequest(
        @NotBlank(message = Messages.BOOK_TITLE_VALIDATION_MESSAGE)
        @Size(max = 150, message = Messages.BOOK_TITLE_LENGTH_VALIDATION_MESSAGE) String title,
        @Min(value = 1, message = Messages.BOOK_PUBLICATION_YEAR_VALIDATION_MESSAGE) Integer publicationYear,
        @NotNull(message = Messages.BOOK_NULL_AUTHORS)
        Set<@NotBlank(message = Messages.AUTHOR_NAME_VALIDATION_MESSAGE)
        @Size(max = 100, message = Messages.AUTHOR_NAME_LENGTH_VALIDATION_MESSAGE) String> authorNames
) {
}
