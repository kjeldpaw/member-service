package dk.wandywharang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Optional;

@Embeddable
public class Address implements dk.wandywharang.api.Address {

    private String street;
    private String city;
    @Column(name = "zip_code")
    private String zipCode;

    public Address() {
    }

    public Address(String street, String city, String zipCode) {
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
    }

    @Override
    public Optional<String> getStreet() {
        return Optional.ofNullable(street);
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Override
    public Optional<String> getCity() {
        return Optional.ofNullable(city);
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public Optional<String> getZipCode() {
        return Optional.ofNullable(zipCode);
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
