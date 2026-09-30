package dk.wandywharang.api;

import lombok.Builder;

@Builder
public record AddMemberReferenceRequest(ReferenceType type, String reference) {
}
