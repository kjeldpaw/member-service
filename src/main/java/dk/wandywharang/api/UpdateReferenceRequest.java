package dk.wandywharang.api;

import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

@Builder
public record UpdateReferenceRequest(@NotEmpty String reference) {
}
