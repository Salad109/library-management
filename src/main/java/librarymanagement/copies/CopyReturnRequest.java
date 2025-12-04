package librarymanagement.copies;

import jakarta.validation.constraints.NotNull;

public record CopyReturnRequest(
        @NotNull Long copyId,
        @NotNull Long customerId
) {
}
