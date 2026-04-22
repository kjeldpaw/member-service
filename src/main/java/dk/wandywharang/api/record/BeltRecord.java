package dk.wandywharang.api.record;

import dk.wandywharang.api.Belt;

import java.time.Duration;
import java.util.UUID;

public record BeltRecord(UUID id, String name, Duration waitTime, Integer rank) implements Belt {

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Duration getWaitTime() {
        return waitTime;
    }

    @Override
    public Integer getRank() {
        return rank;
    }
}
