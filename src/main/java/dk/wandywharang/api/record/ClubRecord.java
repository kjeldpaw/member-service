package dk.wandywharang.api.record;

import dk.wandywharang.api.Address;
import dk.wandywharang.api.Club;

import java.util.UUID;

public record ClubRecord(UUID id, String name, Address address) implements Club {

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Address getAddress() {
        return address;
    }
}
