package dk.wandywharang.api;

import java.util.UUID;

public interface Club {

    UUID getId();

    String getName();

    Address getAddress();
}
