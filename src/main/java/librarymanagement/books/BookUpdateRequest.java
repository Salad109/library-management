package librarymanagement.books;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record BookUpdateRequest(
        @NotBlank @Size(max = 150) String title,
        @Min(1) Integer publicationYear,
        @NotNull Set<@NotBlank @Size(max = 100) String> authorNames
) {
}
