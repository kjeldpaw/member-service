package dk.wandywharang.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Address implements dk.wandywharang.api.Address {

    @Column(nullable = false)
    private String street;
    @Column(nullable = false)
    private String city;
    @Column(name = "zip_code", nullable = false)
    private String zipCode;

    public Address() {
    }

    public Address(String street, String city, String zipCode) {
        this.street = street;
        this.city = city;
        this.zipCode = zipCode;
    }

    @Override
    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @Override
    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    @Override
    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
}
