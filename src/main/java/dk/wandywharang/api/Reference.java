package dk.wandywharang.api;

import java.util.UUID;

public interface Reference {

    UUID getId();

    ReferenceType getType();

    String getReference();
}
