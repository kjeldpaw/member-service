package dk.wandywharang.api.record;

import dk.wandywharang.api.Belt;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.util.UUID;

public record BeltRecord(UUID id, String name, Duration waitTime, Integer rank) implements Belt {

    @Override
    public UUID getId() {
        return id;
    }

    @NotEmpty
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public Duration getWaitTime() {
        return waitTime;
    }

    @NotNull
    @Override
    public Integer getRank() {
        return rank;
    }
}
