package librarymanagement.dto;

import jakarta.validation.constraints.NotNull;

public record MarkLostRequest(@NotNull Long copyId) {
}
