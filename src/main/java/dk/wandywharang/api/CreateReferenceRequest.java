package dk.wandywharang.api;

import lombok.Builder;

@Builder
public record CreateReferenceRequest(ReferenceType type, String reference) {
}
