package librarymanagement.copies;

import jakarta.validation.constraints.NotNull;

public record CopyCheckoutRequest(
        @NotNull Long copyId,
        @NotNull Long customerId
) {
}
