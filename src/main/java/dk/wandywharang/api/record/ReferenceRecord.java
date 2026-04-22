package dk.wandywharang.api.record;

import dk.wandywharang.api.Reference;
import dk.wandywharang.api.ReferenceType;

import java.util.UUID;

public record ReferenceRecord(UUID id, ReferenceType type, String reference) implements Reference {

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public ReferenceType getType() {
        return type;
    }

    @Override
    public String getReference() {
        return reference;
    }
}
