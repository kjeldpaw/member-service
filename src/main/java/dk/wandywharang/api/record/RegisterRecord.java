package dk.wandywharang.api.record;

import dk.wandywharang.api.Register;

public record RegisterRecord(String firstName, String lastName, String email, String password) implements Register {

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }
}
