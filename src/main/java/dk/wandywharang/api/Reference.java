package dk.wandywharang.api;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Reference(UUID id, ReferenceType type, String reference) {
}
