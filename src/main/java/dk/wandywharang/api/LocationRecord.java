package dk.wandywharang.api;

public record LocationRecord(String address, String city, String zipCode) implements Location {
}
