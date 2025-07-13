package librarymanagement.dto;

import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(
        @NotNull Long copyId,
        @NotNull Long customerId
) {
}
