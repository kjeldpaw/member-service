package dk.wandywharang.api;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Club(UUID id, String name, Address address) {
}
