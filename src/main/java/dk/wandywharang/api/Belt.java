package dk.wandywharang.api;

import java.time.Duration;
import java.util.UUID;

public interface Belt {

    UUID getId();

    String getName();

    Duration getWaitTime();

    Integer getRank();
}
