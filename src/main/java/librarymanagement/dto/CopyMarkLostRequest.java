package librarymanagement.dto;

import jakarta.validation.constraints.NotNull;

public record CopyMarkLostRequest(@NotNull Long copyId) {
}
