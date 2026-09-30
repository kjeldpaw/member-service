package dk.wandywharang.api;

import lombok.Builder;

@Builder
public record Address(String street, String city, String zipCode) {
}
