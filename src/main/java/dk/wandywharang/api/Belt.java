package dk.wandywharang.api;

import lombok.Builder;

import java.time.Duration;
import java.util.UUID;

@Builder
public record Belt(UUID id, String name, Duration waitTime, Integer rank) {
}
