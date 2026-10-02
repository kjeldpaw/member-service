package dk.wandywharang.api;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record CreateReferenceRequest(@NotNull ReferenceType type, @NotEmpty String reference) {
}
