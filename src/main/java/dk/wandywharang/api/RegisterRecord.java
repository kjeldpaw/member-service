package dk.wandywharang.api;

public record RegisterRecord(String firstName, String lastName, String email, String password) implements Register {
}
