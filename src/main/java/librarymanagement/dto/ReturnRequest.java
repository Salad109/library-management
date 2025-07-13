package librarymanagement.dto;

import jakarta.validation.constraints.NotNull;

public record ReturnRequest(
        @NotNull Long copyId,
        @NotNull Long customerId
) {
}
