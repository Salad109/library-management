package librarymanagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReservationRequest(
        @NotNull Long customerId,
        @NotBlank String bookIsbn
)
{}
