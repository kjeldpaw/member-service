package dk.wandywharang.api.record;

import dk.wandywharang.api.Address;

public record AddressRecord(String street, String city, String zipCode) implements Address {

    @Override
    public String getStreet() {
        return street;
    }

    @Override
    public String getCity() {
        return city;
    }

    @Override
    public String getZipCode() {
        return zipCode;
    }
}
