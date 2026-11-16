package librarymanagement.copies;

import jakarta.validation.constraints.NotNull;

public record CopyRenewRequest(
        @NotNull Long copyId,
        @NotNull Long customerId
) {
}
