package dk.wandywharang.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public class Location implements dk.wandywharang.api.Location {

    public String address;
    public String city;
    public String zipCode;

    @Override
    public String address() {
        return address;
    }

    @Override
    public String city() {
        return city;
    }

    @Override
    public String zipCode() {
        return zipCode;
    }
}
