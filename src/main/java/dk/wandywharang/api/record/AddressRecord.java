package dk.wandywharang.api.record;

import dk.wandywharang.api.Address;

import java.util.Optional;

public record AddressRecord(String street, String city, String zipCode) implements Address {

    @Override
    public Optional<String> getStreet() {
        return Optional.ofNullable(street);
    }

    @Override
    public Optional<String> getCity() {
        return Optional.ofNullable(city);
    }

    @Override
    public Optional<String> getZipCode() {
        return Optional.ofNullable(zipCode);
    }
}
